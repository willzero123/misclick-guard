/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2026, willzero123 <willzerodev@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.misclickguard;

import com.google.inject.Provides;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Misclick Guard",
	description = "Protects selected interface actions from accidental clicks",
	tags = {"misclick", "menu", "left click", "right click", "auto retaliate"}
)
public class MisclickGuardPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private MisclickGuardConfig config;

	@Inject
	private MouseManager mouseManager;

	private boolean leftPressStartedInMenu;

	private final MouseAdapter mouseListener = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			if (SwingUtilities.isLeftMouseButton(event))
			{
				leftPressStartedInMenu = client.isMenuOpen();
			}
			return event;
		}
	};

	@Provides
	MisclickGuardConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MisclickGuardConfig.class);
	}

	@Override
	protected void startUp()
	{
		mouseManager.registerMouseListener(mouseListener);
	}

	@Override
	protected void shutDown()
	{
		mouseManager.unregisterMouseListener(mouseListener);
		leftPressStartedInMenu = false;
	}

	@Subscribe(priority = -1)
	public void onPostMenuSort(PostMenuSort event)
	{
		removeDisabledEntries();
		hideNoLeftClickTooltip();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		boolean selectedFromOpenMenu = leftPressStartedInMenu;
		leftPressStartedInMenu = false;

		GuardedAction action = GuardedAction.find(event.getMenuEntry());
		if (action == null)
		{
			return;
		}

		ClickMode clickMode = action.clickMode(config);
		if (clickMode.removesMenuEntry()
			|| clickMode.blocksLeftClick() && !selectedFromOpenMenu)
		{
			event.consume();
		}
	}

	private void hideNoLeftClickTooltip()
	{
		MenuEntry[] entries = client.getMenu().getMenuEntries();
		if (entries.length == 0)
		{
			return;
		}

		GuardedAction action = GuardedAction.find(entries[entries.length - 1]);
		if (action == null)
		{
			return;
		}

		ClickMode clickMode = action.clickMode(config);
		if (clickMode.blocksLeftClick() && !clickMode.removesMenuEntry())
		{
			client.setVarcIntValue(VarClientID.TOOLTIP_BUILT, 1);
		}
	}

	private void removeDisabledEntries()
	{
		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		int retainedCount = entries.length;
		for (MenuEntry entry : entries)
		{
			if (shouldRemove(entry))
			{
				retainedCount--;
			}
		}

		if (retainedCount == entries.length)
		{
			return;
		}

		MenuEntry[] retainedEntries = new MenuEntry[retainedCount];
		int retainedIndex = 0;
		for (MenuEntry entry : entries)
		{
			if (!shouldRemove(entry))
			{
				retainedEntries[retainedIndex++] = entry;
			}
		}
		menu.setMenuEntries(retainedEntries);
	}

	private boolean shouldRemove(MenuEntry entry)
	{
		GuardedAction action = GuardedAction.find(entry);
		return action != null && action.clickMode(config).removesMenuEntry();
	}
}
