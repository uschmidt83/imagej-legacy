/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2014 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.legacy.ui;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;

import net.imagej.legacy.DefaultLegacyService;
import net.imagej.legacy.IJ1Helper;
import net.imagej.legacy.LegacyService;
import net.imagej.legacy.display.ImagePlusDisplayViewer;
import net.imagej.legacy.display.LegacyDisplayViewer;

import org.scijava.Priority;
import org.scijava.display.Display;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.AbstractUserInterface;
import org.scijava.ui.Desktop;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.StatusBar;
import org.scijava.ui.SystemClipboard;
import org.scijava.ui.ToolBar;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;
import org.scijava.ui.awt.AWTClipboard;
import org.scijava.ui.swing.SwingUI;
import org.scijava.ui.viewer.DisplayViewer;
import org.scijava.ui.viewer.DisplayWindow;
import org.scijava.widget.FileWidget;

// TODO may want to extend AbstractSwingUI instead.. but many of the implementations
// it provides are overridden here anyway, in the delegation to IJ.
@Plugin(type = UserInterface.class, name = LegacyUI.NAME,
	priority = Priority.HIGH_PRIORITY)
public class LegacyUI extends AbstractUserInterface implements SwingUI {

	public static final String NAME = "legacy";

	@Parameter
	private LegacyService legacyService;

	@Parameter
	private UIService uiService;

	@Parameter
	private LogService log;

	@Parameter
	private PluginService pluginService;

	@Parameter
	private ThreadService threadService;

	private IJ1Helper ij1Helper;
	private Desktop desktop;
	private LegacyApplicationFrame applicationFrame;
	private ToolBar toolBar;

	private StatusBar statusBar;

	private SystemClipboard systemClipboard;

	private IJ1Helper ij1Helper() {
		if (legacyService instanceof DefaultLegacyService) {
			ij1Helper = ((DefaultLegacyService) legacyService).getIJ1Helper();
		}
		return ij1Helper;
	}

	@Override
	public void dispose() {
		// do nothing
	}

	@Override
	public void show() {
		if (ij1Helper() != null) ij1Helper.setVisible(true);
	}

	@Override
	public void show(final Display<?> display) {
		if (uiService.getDisplayViewer(display) != null) {
			// display is already being shown
			return;
		}

		final List<PluginInfo<DisplayViewer<?>>> viewers =
			uiService.getViewerPlugins();

		DisplayViewer<?> displayViewer = null;
		for (final PluginInfo<DisplayViewer<?>> info : viewers) {
			// check that viewer can actually handle the given display
			final DisplayViewer<?> viewer = pluginService.createInstance(info);
			if (viewer == null) continue;
			if (!viewer.canView(display)) continue;
			if (!viewer.isCompatible(this)) continue;
			displayViewer = viewer;
			break; // found a suitable viewer; we are done
		}
		if (displayViewer == null) {
			log.warn("For UI '" + getClass().getName() +
				"': no suitable viewer for display: " + display);
			return;
		}

		if (displayViewer instanceof ImagePlusDisplayViewer) {
			final DisplayViewer<?> finalViewer = displayViewer;
			threadService.queue(new Runnable() {
				@Override
				public void run() {
					finalViewer.view(null, display);
				}
			});
		}
		else {
			super.show(display);
		}
	}

	@Override
	public boolean isVisible() {
		return ij1Helper() != null && ij1Helper.isVisible();
	}

	@Override
	public synchronized Desktop getDesktop() {
		if (desktop != null) return desktop;
		desktop = new Desktop() {

			@Override
			public void setArrangement(Arrangement newValue) {
			}

			@Override
			public Arrangement getArrangement() {
				return null;
			}

			@Override
			public void addPropertyChangeListener(PropertyChangeListener l) {
			}

			@Override
			public void removePropertyChangeListener(PropertyChangeListener l) {
			}

		};
		return desktop;
	}

	@Override
	public synchronized LegacyApplicationFrame getApplicationFrame() {
		if (applicationFrame == null) {
			applicationFrame = new LegacyApplicationFrame(legacyService);
		}
		return applicationFrame;
	}

	@Override
	public synchronized ToolBar getToolBar() {
		if (toolBar == null) {
			toolBar = new LegacyToolBar(legacyService);
		}
		return toolBar;
	}

	@Override
	public synchronized StatusBar getStatusBar() {
		if (statusBar == null) {
			statusBar = new LegacyStatusBar(legacyService);
		}
		return statusBar;
	}

	@Override
	public synchronized SystemClipboard getSystemClipboard() {
		//TODO consider extending abstractAWTUI common class..
		if (systemClipboard != null) return systemClipboard;
		systemClipboard = new AWTClipboard();
		return systemClipboard;
	}

	@Override
	public DisplayWindow createDisplayWindow(Display<?> display) {
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public DialogPrompt dialogPrompt(String message, String title,
			MessageType messageType, OptionType optionType) {
		return new LegacyDialogPrompt(legacyService, message, title, optionType);
	}

	@Override
	public File chooseFile(File file, String style) {
		final JFileChooser chooser = new JFileChooser(file);
		if (FileWidget.DIRECTORY_STYLE.equals(style)) {
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		final int rval;
		if (FileWidget.SAVE_STYLE.equals(style)) {
			rval = chooser.showSaveDialog(getApplicationFrame().getComponent());
		}
		else { // default behavior
			rval = chooser.showOpenDialog(getApplicationFrame().getComponent());
		}
		if (rval != JFileChooser.APPROVE_OPTION) return null;
		return chooser.getSelectedFile();	}

	@Override
	public void showContextMenu(String menuRoot, Display<?> display, int x,
			int y) {
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public void saveLocation() {
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public void restoreLocation() {
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public boolean requiresEDT() {
		return true;
	}
}