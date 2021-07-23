/*
 * zorbage-viewer: utility app for loading and viewing various image data formats
 *
 * Copyright (c) 2020-2021 Barry DeZonia All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * Neither the name of the <copyright holder> nor the names of its contributors may
 * be used to endorse or promote products derived from this software without specific
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package nom.bdezonia.zorbage.viewer;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.math.BigDecimal;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.dataview.PlaneView;
import nom.bdezonia.zorbage.dataview.WindowView;
import nom.bdezonia.zorbage.type.color.ArgbMember;
import nom.bdezonia.zorbage.type.color.RgbMember;
import nom.bdezonia.zorbage.type.color.RgbUtils;

/**
 * 
 * @author Barry DeZonia
 *
 */
public class RgbColorImageViewer<T extends Algebra<T,U>, U> {

	private final T alg;
	private final WindowView<U> view;
	private final BufferedImage argbData;
	private JLabel[] positionLabels;
	private final JFrame frame;
	
	/**
	 * 
	 * @param alg
	 * @param dataSource
	 */
	public RgbColorImageViewer(T alg, DimensionedDataSource<U> dataSource) {
		this(alg, dataSource, 0, 1);
	}

	/**
	 * 
	 * @param alg
	 * @param dataSource
	 * @param c0
	 * @param c1
	 */
	public RgbColorImageViewer(T alg, DimensionedDataSource<U> dataSource, int c0, int c1) {

		this.alg = alg;
		this.view = new WindowView<>(dataSource, 512, 512, c0, c1);
		
		String name = dataSource.getName();
		if (name == null)
			name = "<unknown name>";
		
		String source = dataSource.getSource();
		if (source == null)
			source = "<unknown source>";
		
		frame = new JFrame(name + " " + source);
		
		frame.setLayout(new BorderLayout());
		
		argbData = new BufferedImage(view.d0(), view.d1(), BufferedImage.TYPE_INT_ARGB);
		
		positionLabels = new JLabel[view.getExtraDimsCount()];
		for (int i = 0; i < positionLabels.length; i++) {
			positionLabels[i] = new JLabel();
		}
		
		JPanel graphicsPanel = new JPanel();
		JLabel image = new JLabel(new ImageIcon(argbData));
		JScrollPane scrollPane = new JScrollPane(image);
		graphicsPanel.add(scrollPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		BoxLayout buttonBoxLayout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
		buttonPanel.setLayout(buttonBoxLayout);
		JButton panLeft = new JButton("Left");
		JButton panRight = new JButton("Right");
		JButton panUp = new JButton("Up");
		JButton panDown = new JButton("Down");
		JButton newView = new JButton("New View");
		buttonPanel.add(panLeft);
		buttonPanel.add(panRight);
		buttonPanel.add(panUp);
		buttonPanel.add(panDown);
		buttonPanel.add(newView);
		buttonPanel.add(new JSeparator());
		buttonPanel.add(new JLabel("Dimensions"));
		for (int i = 0; i < dataSource.numDimensions(); i++) {
			String axisName = dataSource.getAxisType(i);
			if (axisName == null)
				axisName = "d" + i;
			buttonPanel.add(new JLabel(dataSource.dimension(i)+" : "+axisName));
		}
		panLeft.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				view.moveWindowLeft(75);
				draw();
				frame.repaint();
			}
		});
		panRight.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				view.moveWindowRight(75);
				draw();
				frame.repaint();
			}
		});
		panUp.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				view.moveWindowUp(75);
				draw();
				frame.repaint();
			}
		});
		panDown.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				view.moveWindowDown(75);
				draw();
				frame.repaint();
			}
		});
		newView.addActionListener(new ActionListener() {
			
			boolean cancelled = false;
			boolean[] checked = new boolean[dataSource.numDimensions()];
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog dlg = new JDialog(frame, "", Dialog.ModalityType.DOCUMENT_MODAL);
				dlg.setLayout(new FlowLayout());
				dlg.add(new JLabel("Choose two dimensions that specify the planes of interest"));
				for (int i = 0; i < dataSource.numDimensions(); i++) {
					checked[i] = false;
					JCheckBox bx = new JCheckBox("d" + i);
					bx.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							String label = bx.getText();
							int dimNum = Integer.parseInt(label.substring(1));
							checked[dimNum] = !checked[dimNum];
						}
					});
					dlg.add(bx);
				}
				JButton ok = new JButton("Ok");
				ok.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						cancelled = false;
						dlg.setVisible(false);
					}
				});
				JButton cancel = new JButton("Cancel");
				cancel.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						cancelled = true;
						dlg.setVisible(false);
					}
				});
				dlg.add(ok);
				dlg.add(cancel);
				dlg.pack();
				dlg.setVisible(true);
				if (!cancelled) {
					int i0 = -1;
					int i1 = -1;
					int iOthers = -1;
					for (int i = 0; i < checked.length; i++) {
						if (checked[i]) {
							if (i0 == -1) i0 = i;
							else if (i1 == -1) i1 = i;
							else iOthers = i;
						}
					}
					// make sure only two dims were chosen
					if (i0 != -1 && i1 != -1 && iOthers == -1)
						new RgbColorImageViewer<>(alg, dataSource, i0, i1);
					//else
					//	System.out.println("" + i0 + " " + i1 + " " + iOthers);
				}
			}
		});
		
		JPanel positions = new JPanel();
		BoxLayout positionsBoxLayout = new BoxLayout(positions, BoxLayout.Y_AXIS);
		positions.setLayout(positionsBoxLayout);
		
		for (int i = 0; i < view.getExtraDimsCount(); i++) {
			JPanel miniPanel = new JPanel();
			miniPanel.setLayout(new FlowLayout());
			JButton decrementButton = new JButton("Decrement");
			JButton incrementButton = new JButton("Increment");
			PlaneView<U> pView = view.getPlaneView();
			long maxVal = pView.originalCoordDim(i);
			positionLabels[i].setText(""+(view.getExtraDimValue(i)+1)+" / "+maxVal);
			int pos = pView.originalCoordPos(i);
			String axisLabel;
			if (view.getDataSource().getAxisType(pos) == null)
				axisLabel = "" + pos + " : ";
			else
				axisLabel = view.getDataSource().getAxisType(pos) + " : ";
			miniPanel.add(new JLabel(axisLabel));
			miniPanel.add(decrementButton);
			miniPanel.add(positionLabels[i]);
			miniPanel.add(incrementButton);
			positions.add(miniPanel);
			decrementButton.addActionListener(new Decrementer(i));
			incrementButton.addActionListener(new Incrementer(i));
		}
		
		JPanel statusPanel = new JPanel(); 
		JLabel readout = new JLabel();
		scrollPane.addMouseMotionListener(new MouseMotionListener() {
			
			long[] modelCoords = new long[dataSource.numDimensions()];
			BigDecimal[] realWorldCoords = new BigDecimal[dataSource.numDimensions()]; 
			U value = alg.construct();
			
			@Override
			public void mouseMoved(MouseEvent e) {
				int i0 = e.getX();
				int i1 = e.getY();
				if (i0 >= 0 && i0 < view.d0() && i1 >= 0 && i1 < view.d1()) {
					int a, r, g, b;
					ArgbMember argbMem = null;
					if (value instanceof ArgbMember)
						argbMem = (ArgbMember) value;
					RgbMember rgbMem = null;
					if (value instanceof RgbMember)
						rgbMem = (RgbMember) value;
					view.getModelCoords(i0, i1, modelCoords);
					dataSource.getCoordinateSpace().project(modelCoords, realWorldCoords);
					long dataU = view.origin0() + i0;
					long dataV = view.origin1() + i1;
					view.get(i0, i1, value);
					if (argbMem != null) {
						a = argbMem.a();
						r = argbMem.r();
						g = argbMem.g();
						b = argbMem.b();
					}
					else if (rgbMem != null) {
						a = 255;
						r = rgbMem.r();
						g = rgbMem.g();
						b = rgbMem.b();
					}
					else {
						throw new IllegalArgumentException("bad error: color is not ARGB or RGB");
					}
					int c0 = view.getPlaneView().c0();
					int c1 = view.getPlaneView().c1();
					StringBuilder sb = new StringBuilder();
					sb.append(dataSource.getAxisType(c0) == null ? "d0" : dataSource.getAxisType(c0));
					sb.append(" = ");
					sb.append(dataU);
					// only display calibrated values if they are not == 1.0 * uncalibrated values
					if (realWorldCoords[c0].remainder(BigDecimal.valueOf(modelCoords[c0])).compareTo(BigDecimal.valueOf(0.0000000001)) < 0) {
						sb.append(" (");
						sb.append(realWorldCoords[c0]);
						sb.append(" ");
						sb.append(dataSource.getAxisUnit(c0) == null ? "" : dataSource.getAxisUnit(c0));
						sb.append(")");
					}
					sb.append(", ");
					sb.append(dataSource.getAxisType(c1) == null ? "d1" : dataSource.getAxisType(c1));
					sb.append("= ");
					sb.append(dataV);
					// only display calibrated values if they are not == 1.0 * uncalibrated values
					if (realWorldCoords[c1].remainder(BigDecimal.valueOf(modelCoords[c1])).compareTo(BigDecimal.valueOf(0.0000000001)) < 0) {
						sb.append(" (");
						sb.append(realWorldCoords[c1]);
						sb.append(" ");
						sb.append(dataSource.getAxisUnit(c1) == null ? "" : dataSource.getAxisUnit(c1));
						sb.append(")");
					}
					sb.append(", value = ");
					sb.append("(");
					sb.append(a);
					sb.append(",");
					sb.append(r);
					sb.append(",");
					sb.append(g);
					sb.append(",");
					sb.append(b);
					sb.append(")");
					readout.setText(sb.toString());
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
			}
		});
		statusPanel.add(readout, BorderLayout.CENTER);
		
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BorderLayout());
		controlPanel.add(positions, BorderLayout.CENTER);

		frame.add(statusPanel, BorderLayout.NORTH);
		frame.add(graphicsPanel, BorderLayout.CENTER);
		frame.add(buttonPanel, BorderLayout.EAST);
		frame.add(controlPanel, BorderLayout.SOUTH);
		
		frame.pack();

		frame.setVisible(true);

		draw();
		
		frame.repaint();
	}
	
	private class Incrementer implements ActionListener {
		
		private final int extraPos;
		
		public Incrementer(int extraNum) {
			extraPos = extraNum;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// find dim pos in real world data source of extra dims pos i
			PlaneView<U> pView = view.getPlaneView();
			long maxVal = pView.originalCoordDim(extraPos);
			long pos = view.getExtraDimValue(extraPos);
			if (pos < maxVal - 1) {
				pos++;
				view.setExtraDimValue(extraPos, pos);
				positionLabels[extraPos].setText(""+(pos+1)+" / "+maxVal);
				draw();
				frame.repaint();
			}
		}
	}
	
	private class Decrementer implements ActionListener {
		
		private final int extraPos;
		
		public Decrementer(int extraNum) {
			extraPos = extraNum;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			PlaneView<U> pView = view.getPlaneView();
			long maxVal = pView.originalCoordDim(extraPos);
			long pos = view.getExtraDimValue(extraPos);
			if (pos > 0) {
				pos--;
				view.setExtraDimValue(extraPos, pos);
				positionLabels[extraPos].setText(""+(pos+1)+" / "+maxVal);
				draw();
				frame.repaint();
			}
		}
	}
	
	private void draw() {
		U value = alg.construct();
		
		// Safe cast as img is of correct type 
		
		DataBufferInt buffer = (DataBufferInt) argbData.getRaster().getDataBuffer();

		// Conveniently, the buffer already contains the data array
		int[] arrayInt = buffer.getData();

		int v;
		
		for (int y = 0; y < view.d1(); y++) {
			for (int x = 0; x < view.d0(); x++) {

				view.get(x, y, value);
				
				if (value instanceof RgbMember) {
					RgbMember rgb = (RgbMember) value;
					v = RgbUtils.argb(255, rgb.r(), rgb.g(), rgb.b());
				}
				else if (value instanceof ArgbMember) {
					ArgbMember argb = (ArgbMember) value;
					v = RgbUtils.argb(argb.a(), argb.r(), argb.g(), argb.b());
				}
				else {
					throw new IllegalArgumentException("unknown color type");
				}

				// put a color from the color table into the image at pos (x,y)
				
				int bufferPos = y * view.d0() + x;
				
				arrayInt[bufferPos] = v;
			}
		}
	}
}
