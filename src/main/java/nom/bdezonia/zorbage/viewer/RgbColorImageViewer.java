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
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.math.BigDecimal;
import java.text.DecimalFormat;

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
	private final JLabel[] positionLabels;
	private final JFrame frame;
	
	/**
	 * Make an interactive graphical viewer for a a/rgb data source.
	 * @param alg The algebra that matches the type of data to display
	 * @param dataSource The data to display
	 */
	public RgbColorImageViewer(T alg, DimensionedDataSource<U> dataSource) {
		this(alg, dataSource, 0, 1);
	}

	/**
	 * Make an interactive graphical viewer for an a/rgb data source.
	 * @param alg The algebra that matches the type of data to display
	 * @param dataSource The data to display
	 * @param axisNumber0 The first axis number defining the planes to view (x, y, z, c, t, etc.)
	 * @param axisNumber1 The second axis number defining the planes to view (x, y, z, c, t, etc.)
	 */
	public RgbColorImageViewer(T alg, DimensionedDataSource<U> dataSource, int axisNumber0, int axisNumber1) {

		this.alg = alg;
		this.view = new WindowView<>(dataSource, 512, 512, axisNumber0, axisNumber1);
		
		String name = dataSource.getName();
		if (name == null)
			name = "<unknown name>";
		
		String source = dataSource.getSource();
		if (source == null)
			source = "<unknown source>";
		
		String dataType = dataSource.getValueType();
		String dataUnit = dataSource.getValueUnit();

		String title = "Zorbage Viewer - "+name;
	
		String valueInfo = "<unknown type>";
		if (dataType != null)
			valueInfo = dataType;
		String valueUnit = "(<unknown unit>)";
		if (dataUnit != null)
			valueUnit = " (" + dataUnit + ")";
		String valueString = valueInfo + valueUnit;
		
		frame = new JFrame(title);
		
		frame.setLayout(new BorderLayout());
		
		argbData = new BufferedImage(view.d0(), view.d1(), BufferedImage.TYPE_INT_ARGB);
		
		positionLabels = new JLabel[view.getPositionsCount()];
		for (int i = 0; i < positionLabels.length; i++) {
			positionLabels[i] = new JLabel();
		}
		
		JLabel sourceLabel = new JLabel("Source: "+source);
		sourceLabel.setBackground(Color.WHITE);
		sourceLabel.setOpaque(true);

		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.add(sourceLabel);
		headerPanel.add(new JLabel(alg.typeDescription()));
		headerPanel.add(new JLabel(valueString));
		
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
		JButton snapshot = new JButton("Snapshot");
		buttonPanel.add(panLeft);
		buttonPanel.add(panRight);
		buttonPanel.add(panUp);
		buttonPanel.add(panDown);
		buttonPanel.add(newView);
		buttonPanel.add(snapshot);
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
						new RealImageViewer<>(alg, dataSource, i0, i1);
					//else
					//	System.out.println("" + i0 + " " + i1 + " " + iOthers);
				}
			}
		});
		snapshot.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {

				DimensionedDataSource<U> snap = view.takeSnapsot(alg.construct());
				
				new RealImageViewer<>(alg, snap);
			}
		});
		
		JPanel positions = new JPanel();
		BoxLayout positionsBoxLayout = new BoxLayout(positions, BoxLayout.Y_AXIS);
		positions.setLayout(positionsBoxLayout);
		
		for (int i = 0; i < view.getPositionsCount(); i++) {
			JPanel miniPanel = new JPanel();
			miniPanel.setLayout(new FlowLayout());
			JButton homeButton = new JButton("<<");
			JButton decrementButton = new JButton("<");
			JButton incrementButton = new JButton(">");
			JButton endButton = new JButton(">>");
			PlaneView<U> pView = view.getPlaneView();
			long maxVal = pView.getDataSourceAxisSize(i);
			positionLabels[i].setText(""+(view.getPositionValue(i)+1)+" / "+maxVal);
			int pos = pView.getDataSourceAxisNumber(i);
			String axisLabel;
			if (view.getDataSource().getAxisType(pos) == null)
				axisLabel = "" + pos + " : ";
			else
				axisLabel = view.getDataSource().getAxisType(pos) + " : ";
			miniPanel.add(new JLabel(axisLabel));
			miniPanel.add(positionLabels[i]);
			miniPanel.add(decrementButton);
			miniPanel.add(incrementButton);
			miniPanel.add(homeButton);
			miniPanel.add(endButton);
			positions.add(miniPanel);
			decrementButton.addActionListener(new Decrementer(i));
			incrementButton.addActionListener(new Incrementer(i));
			homeButton.addActionListener(new Home(i));
			endButton.addActionListener(new End(i));
		}

		JLabel readout = new JLabel();
		readout.setBackground(Color.WHITE);
		readout.setOpaque(true);
		readout.setText("<placeholder>");
		scrollPane.addMouseMotionListener(new MouseMotionListener() {

			long[] modelCoords = new long[dataSource.numDimensions()];
			BigDecimal[] realWorldCoords = new BigDecimal[dataSource.numDimensions()]; 
			U value = alg.construct();
			DecimalFormat df = new DecimalFormat("0.000");

			@Override
			public void mouseMoved(MouseEvent e) {
				ArgbMember argb = null;
				if (value instanceof ArgbMember)
					argb = (ArgbMember) value;
				RgbMember rgb = null;
				if (value instanceof RgbMember)
					rgb = (RgbMember) value;
				int i0 = e.getX();
				int i1 = e.getY();
				if (i0 >= 0 && i0 < view.d0() && i1 >= 0 && i1 < view.d1()) {
					view.getModelCoords(i0, i1, modelCoords);
					dataSource.getCoordinateSpace().project(modelCoords, realWorldCoords);
					long dataU = view.origin0() + i0;
					long dataV = view.origin1() + i1;
					view.get(i0, i1, value);
					String componentString = "";
					if (argb != null) {
						StringBuilder b = new StringBuilder();
						b.append('(');
						b.append(argb.a());
						b.append(',');
						b.append(argb.r());
						b.append(',');
						b.append(argb.g());
						b.append(',');
						b.append(argb.b());
						b.append(')');
						componentString = b.toString();
					}
					if (rgb != null) {
						StringBuilder b = new StringBuilder();
						b.append('(');
						b.append(rgb.r());
						b.append(',');
						b.append(rgb.g());
						b.append(',');
						b.append(rgb.b());
						b.append(')');
						componentString = b.toString();
					}
					int axisNumber0 = view.getPlaneView().axisNumber0();
					int axisNumber1 = view.getPlaneView().axisNumber1();
					StringBuilder sb = new StringBuilder();
					sb.append(dataSource.getAxisType(axisNumber0) == null ? "d0" : dataSource.getAxisType(axisNumber0));
					sb.append(" = ");
					sb.append(dataU);
					// only display calibrated values if they are not == 1.0 * uncalibrated values
					if (realWorldCoords[axisNumber0].subtract(BigDecimal.valueOf(modelCoords[axisNumber0])).abs().compareTo(BigDecimal.valueOf(0.000001)) > 0) {
						sb.append(" (");
						sb.append(df.format(realWorldCoords[axisNumber0]));
						sb.append(" ");
						sb.append(dataSource.getAxisUnit(axisNumber0) == null ? "" : dataSource.getAxisUnit(axisNumber0));
						sb.append(")");
					}
					sb.append(", ");
					sb.append(dataSource.getAxisType(axisNumber1) == null ? "d1" : dataSource.getAxisType(axisNumber1));
					sb.append("= ");
					sb.append(dataV);
					// only display calibrated values if they are not == 1.0 * uncalibrated values
					if (realWorldCoords[axisNumber1].subtract(BigDecimal.valueOf(modelCoords[axisNumber1])).abs().compareTo(BigDecimal.valueOf(0.000001)) > 0) {
						sb.append(" (");
						sb.append(df.format(realWorldCoords[axisNumber1]));
						sb.append(" ");
						sb.append(dataSource.getAxisUnit(axisNumber1) == null ? "" : dataSource.getAxisUnit(axisNumber1));
						sb.append(")");
					}
					sb.append(", value = ");
					sb.append(componentString);
					readout.setText(sb.toString());
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
			}
		});
		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new BorderLayout());
		sliderPanel.add(readout, BorderLayout.NORTH);
		sliderPanel.add(positions, BorderLayout.CENTER);

		frame.add(headerPanel, BorderLayout.NORTH);
		frame.add(graphicsPanel, BorderLayout.CENTER);
		frame.add(buttonPanel, BorderLayout.EAST);
		frame.add(sliderPanel, BorderLayout.SOUTH);
		
		frame.pack();

		frame.setVisible(true);

		draw();
		
		frame.repaint();
	}
	
	// code to increment a slider and react
	
	private class Incrementer implements ActionListener {
		
		private final int extraPos;
		
		public Incrementer(int extraNum) {
			extraPos = extraNum;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// find dim pos in real world data source of extra dims pos i
			PlaneView<U> pView = view.getPlaneView();
			long maxVal = pView.getDataSourceAxisSize(extraPos);
			long pos = view.getPositionValue(extraPos);
			if (pos < maxVal - 1) {
				pos++;
				view.setPositionValue(extraPos, pos);
				positionLabels[extraPos].setText(""+(pos+1)+" / "+maxVal);
				draw();
				frame.repaint();
			}
		}
	}
	
	// code to decrement a slider and react
	
	private class Decrementer implements ActionListener {
		
		private final int extraPos;
		
		public Decrementer(int extraNum) {
			extraPos = extraNum;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			PlaneView<U> pView = view.getPlaneView();
			long maxVal = pView.getDataSourceAxisSize(extraPos);
			long pos = view.getPositionValue(extraPos);
			if (pos > 0) {
				pos--;
				view.setPositionValue(extraPos, pos);
				positionLabels[extraPos].setText(""+(pos+1)+" / "+maxVal);
				draw();
				frame.repaint();
			}
		}
	}
	
	// code to set a slider to zero and react
	
	private class Home implements ActionListener {
		
		private final int extraPos;
		
		public Home(int extraNum) {
			extraPos = extraNum;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			PlaneView<U> pView = view.getPlaneView();
			long maxVal = pView.getDataSourceAxisSize(extraPos);
			view.setPositionValue(extraPos, 0);
			positionLabels[extraPos].setText(""+(1)+" / "+maxVal);
			draw();
			frame.repaint();
		}
	}
	
	// code to set a slider to its max value and react
	
	private class End implements ActionListener {
		
		private final int extraPos;
		
		public End(int extraNum) {
			extraPos = extraNum;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			PlaneView<U> pView = view.getPlaneView();
			long maxVal = pView.getDataSourceAxisSize(extraPos);
			view.setPositionValue(extraPos, maxVal-1);
			positionLabels[extraPos].setText(""+(maxVal)+" / "+maxVal);
			draw();
			frame.repaint();
		}
	}
	
	// draw the data
	
	private void draw() {

		U value = alg.construct();

		ArgbMember argb = null;
		if (value instanceof ArgbMember) {
			argb = (ArgbMember) value;
		}
		
		RgbMember rgb = null;
		if (value instanceof RgbMember) {
			rgb = (RgbMember) value;
		}
		
		// Safe cast as img is of correct type 
		
		DataBufferInt buffer = (DataBufferInt) argbData.getRaster().getDataBuffer();

		// Conveniently, the buffer already contains the data array
		int[] arrayInt = buffer.getData();

		for (int y = 0; y < view.d1(); y++) {
			for (int x = 0; x < view.d0(); x++) {

				view.get(x, y, value);

				int color = 0;
				
				if (argb != null) {
					color = RgbUtils.argb(argb.a(), argb.r(), argb.g(), argb.b());
				}
				
				if (rgb != null) {
					color = RgbUtils.argb(255, rgb.r(), rgb.g(), rgb.b());
				}

				// put the color into the image at pos (x,y)
				
				int bufferPos = y * view.d0() + x;
				
				arrayInt[bufferPos] = color;
			}
		}
	}
}
