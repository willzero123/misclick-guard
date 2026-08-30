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

import java.util.EnumSet;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;

enum GuardedAction
{
	AUTO_RETALIATE(
		MisclickGuardConfig::autoRetaliate,
		MenuPattern.option("auto retaliate"),
		MenuPattern.target("auto retaliate")
	),
	HIDE_XP_DROPS(
		MisclickGuardConfig::xpDrops,
		MenuPattern.option("hide xp drops"),
		MenuPattern.optionAndTarget("hide", "xp drops")
	),
	SHOW_XP_DROPS(
		MisclickGuardConfig::xpDrops,
		MenuPattern.option("show xp drops"),
		MenuPattern.optionAndTarget("show", "xp drops")
	),
	SETUP_XP_DROPS(
		MisclickGuardConfig::xpDrops,
		MenuPattern.option("setup xp drops"),
		MenuPattern.optionAndTarget("setup", "xp drops")
	),
	ALWAYS_SET_PLACEHOLDERS(
		MisclickGuardConfig::alwaysSetPlaceholders,
		MenuPattern.option("always set placeholders"),
		MenuPattern.target("always set placeholders")
	);

	private static final GuardedAction[] ACTIONS = values();
	private static final EnumSet<MenuAction> WIDGET_ACTIONS = EnumSet.of(
		MenuAction.CC_OP,
		MenuAction.CC_OP_LOW_PRIORITY,
		MenuAction.WIDGET_TYPE_1,
		MenuAction.WIDGET_TYPE_4,
		MenuAction.WIDGET_TYPE_5,
		MenuAction.WIDGET_FIRST_OPTION,
		MenuAction.WIDGET_SECOND_OPTION,
		MenuAction.WIDGET_THIRD_OPTION,
		MenuAction.WIDGET_FOURTH_OPTION,
		MenuAction.WIDGET_FIFTH_OPTION
	);

	private final Function<MisclickGuardConfig, ClickMode> clickMode;
	private final MenuPattern[] menuPatterns;

	GuardedAction(
		Function<MisclickGuardConfig, ClickMode> clickMode,
		MenuPattern... menuPatterns)
	{
		this.clickMode = clickMode;
		this.menuPatterns = menuPatterns;
	}

	ClickMode clickMode(MisclickGuardConfig config)
	{
		return clickMode.apply(config);
	}

	@Nullable
	static GuardedAction find(MenuEntry entry)
	{
		if (entry == null || !WIDGET_ACTIONS.contains(entry.getType()))
		{
			return null;
		}

		for (GuardedAction action : ACTIONS)
		{
			if (action.matches(entry))
			{
				return action;
			}
		}
		return null;
	}

	private boolean matches(MenuEntry entry)
	{
		for (MenuPattern pattern : menuPatterns)
		{
			if (pattern.matches(entry))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean labelEquals(String actual, String expected)
	{
		if (actual == null)
		{
			return false;
		}

		int expectedIndex = 0;
		boolean inTag = false;
		for (int i = 0; i < actual.length(); i++)
		{
			char current = actual.charAt(i);
			if (current == '<')
			{
				inTag = true;
				continue;
			}
			if (inTag)
			{
				if (current == '>')
				{
					inTag = false;
				}
				continue;
			}
			if (expectedIndex >= expected.length()
				|| Character.toLowerCase(current) != expected.charAt(expectedIndex++))
			{
				return false;
			}
		}
		return !inTag && expectedIndex == expected.length();
	}

	private static final class MenuPattern
	{
		@Nullable
		private final String option;

		@Nullable
		private final String target;

		private MenuPattern(@Nullable String option, @Nullable String target)
		{
			this.option = option;
			this.target = target;
		}

		private static MenuPattern option(String option)
		{
			return new MenuPattern(option, null);
		}

		private static MenuPattern target(String target)
		{
			return new MenuPattern(null, target);
		}

		private static MenuPattern optionAndTarget(String option, String target)
		{
			return new MenuPattern(option, target);
		}

		private boolean matches(MenuEntry entry)
		{
			return (option == null || labelEquals(entry.getOption(), option))
				&& (target == null || labelEquals(entry.getTarget(), target));
		}
	}
}
