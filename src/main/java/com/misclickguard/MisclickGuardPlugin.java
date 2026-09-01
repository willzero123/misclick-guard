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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
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

	private final AtomicBoolean leftPressStartedInMenu = new AtomicBoolean();
	private boolean autoRetaliateTooltipHiddenByPlugin;
	private boolean autoRetaliateTooltipWasHidden;

	private final MouseAdapter mouseListener = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			if (SwingUtilities.isLeftMouseButton(event))
			{
				leftPressStartedInMenu.set(client.isMenuOpen());
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
		restoreAutoRetaliateTooltip();
		leftPressStartedInMenu.set(false);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuEntry entry = event.getMenuEntry();
		GuardedAction action = GuardedAction.find(entry);
		if (action == null)
		{
			return;
		}

		ClickMode clickMode = action.clickMode(config);
		if (config.deprioritizeLeftClickOffEntries()
			&& clickMode.blocksLeftClick() && !clickMode.removesMenuEntry())
		{
			entry.setDeprioritized(true);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (MisclickGuardConfig.GROUP.equals(event.getGroup()))
		{
			restoreAutoRetaliateTooltip();
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		restoreCancelForOpenMenu();
	}

	@Subscribe(priority = -1)
	public void onPostMenuSort(PostMenuSort event)
	{
		MenuEntry[] entries = client.getMenu().getMenuEntries();
		updateAutoRetaliateTooltip(shouldHideAutoRetaliateTooltip(entries));
		removeDisabledEntries();
		makeCancelDefaultForDeprioritizedEntry();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		boolean selectedFromOpenMenu = leftPressStartedInMenu.getAndSet(false);

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

	private boolean shouldHideAutoRetaliateTooltip(MenuEntry[] entries)
	{
		for (MenuEntry entry : entries)
		{
			if (GuardedAction.find(entry) != GuardedAction.AUTO_RETALIATE)
			{
				continue;
			}

			ClickMode clickMode = GuardedAction.AUTO_RETALIATE.clickMode(config);
			return clickMode.removesMenuEntry()
				|| config.deprioritizeLeftClickOffEntries() && clickMode.blocksLeftClick();
		}
		return false;
	}

	private void updateAutoRetaliateTooltip(boolean hide)
	{
		if (hide)
		{
			hideAutoRetaliateTooltip();
		}
		else
		{
			restoreAutoRetaliateTooltip();
		}
	}

	private void hideAutoRetaliateTooltip()
	{
		Widget tooltip = client.getWidget(InterfaceID.CombatInterface.TOOLTIP);
		if (tooltip == null)
		{
			return;
		}

		if (!autoRetaliateTooltipHiddenByPlugin)
		{
			autoRetaliateTooltipWasHidden = tooltip.isHidden();
			autoRetaliateTooltipHiddenByPlugin = true;
		}
		tooltip.setHidden(true);
	}

	private void restoreAutoRetaliateTooltip()
	{
		if (!autoRetaliateTooltipHiddenByPlugin)
		{
			return;
		}

		Widget tooltip = client.getWidget(InterfaceID.CombatInterface.TOOLTIP);
		if (tooltip != null)
		{
			tooltip.setHidden(autoRetaliateTooltipWasHidden);
		}
		autoRetaliateTooltipHiddenByPlugin = false;
	}

	private void makeCancelDefaultForDeprioritizedEntry()
	{
		if (!config.deprioritizeLeftClickOffEntries())
		{
			return;
		}

		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		if (entries.length == 0)
		{
			return;
		}

		MenuEntry topEntry = entries[entries.length - 1];
		GuardedAction action = GuardedAction.find(topEntry);
		if (action == null)
		{
			return;
		}

		ClickMode clickMode = action.clickMode(config);
		if (!topEntry.isDeprioritized()
			|| !clickMode.blocksLeftClick() || clickMode.removesMenuEntry())
		{
			return;
		}

		for (int i = 0; i < entries.length; i++)
		{
			if (entries[i].getType() == MenuAction.CANCEL)
			{
				MenuEntry cancelEntry = entries[i];
				cancelEntry.setOption("");
				System.arraycopy(entries, i + 1, entries, i, entries.length - i - 1);
				entries[entries.length - 1] = cancelEntry;
				menu.setMenuEntries(entries);
				return;
			}
		}

		menu.createMenuEntry(-1)
			.setOption("")
			.setTarget("")
			.setType(MenuAction.CANCEL);
	}

	private void restoreCancelForOpenMenu()
	{
		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		for (int i = 0; i < entries.length; i++)
		{
			MenuEntry entry = entries[i];
			if (entry.getType() != MenuAction.CANCEL || !"".equals(entry.getOption()))
			{
				continue;
			}

			entry.setOption("Cancel");
			if (i > 0)
			{
				System.arraycopy(entries, 0, entries, 1, i);
				entries[0] = entry;
				menu.setMenuEntries(entries);
			}
			return;
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
