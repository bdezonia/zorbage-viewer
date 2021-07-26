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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algebra.Bounded;
import nom.bdezonia.zorbage.algebra.G;
import nom.bdezonia.zorbage.algebra.HighPrecRepresentation;
import nom.bdezonia.zorbage.algebra.Infinite;
import nom.bdezonia.zorbage.algebra.NaN;
import nom.bdezonia.zorbage.algebra.Ordered;
import nom.bdezonia.zorbage.algorithm.MinMaxElement;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.datasource.IndexedDataSource;
import nom.bdezonia.zorbage.dataview.PlaneView;
import nom.bdezonia.zorbage.dataview.WindowView;
import nom.bdezonia.zorbage.misc.BigDecimalUtils;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionAlgebra;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionMember;

/**
 * 
 * @author Barry DeZonia
 *
 */
public class RealImageViewer<T extends Algebra<T,U>, U> {

	private final T alg;
	private final WindowView<U> view;
	private final BufferedImage argbData;
	private int[] colorTable = LutUtils.DEFAULT_COLOR_TABLE;
	private boolean preferDataRange = true;
	private final U min;
	private final U max;
	private final JLabel[] positionLabels;
	private final JFrame frame;
	private int scale = 1;
	private NaN<U> nanTester = null;
	private Infinite<U> infTester = null;
	private Ordered<U> signumTester = null;

	
	/**
	 * Make an interactive graphical viewer for a real data source.
	 * @param alg The algebra that matches the type of data to display
	 * @param dataSource The data to display
	 */
	public RealImageViewer(T alg, DimensionedDataSource<U> dataSource) {
		this(alg, dataSource, 0, 1);
	}

	/**
	 * Make an interactive graphical viewer for a real data source.
	 * @param alg The algebra that matches the type of data to display
	 * @param dataSource The data to display
	 * @param axisNumber0 The first axis number defining the planes to view (x, y, z, c, t, etc.)
	 * @param axisNumber1 The second axis number defining the planes to view (x, y, z, c, t, etc.)
	 */
	public RealImageViewer(T alg, DimensionedDataSource<U> dataSource, int axisNumber0, int axisNumber1) {

		this.alg = alg;
		this.view = new WindowView<>(dataSource, 512, 512, axisNumber0, axisNumber1);
		this.min = alg.construct();
		this.max = alg.construct();

		if (alg instanceof NaN) {
			this.nanTester = (NaN<U>) alg;
		}
		if (alg instanceof Infinite) {
			this.infTester = (Infinite<U>) alg;
		}
		if (alg instanceof Ordered) {
			this.signumTester = (Ordered<U>) alg;
		}
		else {
			throw new IllegalArgumentException("Weird error: very strange real number type that is not ordered!");
		}

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
		JButton panReset = new JButton("Reset");
		JButton loadLut = new JButton("Load LUT");
		JButton resetLut = new JButton("Reset LUT");
		JButton newView = new JButton("New View");
		JButton snapshot = new JButton("Snapshot");
		JCheckBox check = new JCheckBox("Use data range");
		check.setSelected(preferDataRange);
		buttonPanel.add(panLeft);
		buttonPanel.add(panRight);
		buttonPanel.add(panUp);
		buttonPanel.add(panDown);
		buttonPanel.add(panReset);
		buttonPanel.add(loadLut);
		buttonPanel.add(resetLut);
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
		buttonPanel.add(check);
		check.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				preferDataRange = !preferDataRange;
				calcMinMax();
				draw();
				frame.repaint();
			}
		});
		panLeft.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (view.origin0() <= 0) return;
				int amount = 75 / scale;
				if (amount == 0) amount = 1;
				view.moveWindowLeft(amount);
				draw();
				frame.repaint();
			}
		});
		panRight.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (view.origin0() >= view.getDataSource().dimension(axisNumber0) - view.d0()) return;
				int amount = 75 / scale;
				if (amount == 0) amount = 1;
				view.moveWindowRight(amount);
				draw();
				frame.repaint();
			}
		});
		panUp.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (view.origin1() <= 0) return;
				int amount = 75 / scale;
				if (amount == 0) amount = 1;
				view.moveWindowUp(amount);
				draw();
				frame.repaint();
			}
		});
		panDown.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (view.origin1() >= view.getDataSource().dimension(axisNumber1) - view.d1()) return;
				int amount = 75 / scale;
				if (amount == 0) amount = 1;
				view.moveWindowDown(amount);
				draw();
				frame.repaint();
			}
		});
		panReset.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				view.moveWindowLeft(view.origin0());
				view.moveWindowUp(view.origin1());
				draw();
				frame.repaint();
			}
		});
		loadLut.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser();
				int retVal = jfc.showOpenDialog(frame);
				if (retVal == 0) {
					setColorTable(LutUtils.loadLUT(jfc.getSelectedFile()));
				}
			}
		});
		resetLut.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setColorTable(LutUtils.DEFAULT_COLOR_TABLE);
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
			miniPanel.add(homeButton);
			miniPanel.add(decrementButton);
			miniPanel.add(incrementButton);
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

			HighPrecisionMember hpVal = G.HP.construct();
			long[] modelCoords = new long[dataSource.numDimensions()];
			BigDecimal[] realWorldCoords = new BigDecimal[dataSource.numDimensions()]; 
			U value = alg.construct();
			DecimalFormat df = new DecimalFormat("0.000");
					
			@Override
			public void mouseMoved(MouseEvent e) {
				boolean troubleAxis;
				String alternateValue = null;
				int i0 = e.getX() / scale;
				int i1 = e.getY() / scale;
				if (i0 >= 0 && i0 < view.d0() && i1 >= 0 && i1 < view.d1()) {
					view.getModelCoords(i0, i1, modelCoords);
					dataSource.getCoordinateSpace().project(modelCoords, realWorldCoords);
					long dataU = view.origin0() + i0;
					long dataV = view.origin1() + i1;
					view.get(i0, i1, value);
					if ((nanTester != null) && nanTester.isNaN().call(value)) {
						alternateValue = "nan";
					}
					else if ((infTester != null) && infTester.isInfinite().call(value)) {
						
						if (signumTester.signum().call(value) <= 0)
							alternateValue = "-Inf";
						else
							alternateValue = "+Inf";
					}
					else {
						alternateValue = null;
						HighPrecRepresentation rep = (HighPrecRepresentation) value;
						rep.toHighPrec(hpVal);
					}
					int axisNumber0 = view.getPlaneView().axisNumber0();
					int axisNumber1 = view.getPlaneView().axisNumber1();
					StringBuilder sb = new StringBuilder();
					troubleAxis = (axisNumber0 >= dataSource.numDimensions() || dataSource.getAxisType(axisNumber0) == null);
					sb.append(troubleAxis ? "d0" : dataSource.getAxisType(axisNumber0));
					sb.append(" = ");
					sb.append(dataU);
					// only display calibrated values if they are not == 1.0 * uncalibrated values
					if (axisNumber0 < dataSource.numDimensions()) {
						if (realWorldCoords[axisNumber0].subtract(BigDecimal.valueOf(modelCoords[axisNumber0])).abs().compareTo(BigDecimal.valueOf(0.000001)) > 0) {
							sb.append(" (");
							sb.append(df.format(realWorldCoords[axisNumber0]));
							sb.append(" ");
							sb.append(dataSource.getAxisUnit(axisNumber0) == null ? "" : dataSource.getAxisUnit(axisNumber0));
							sb.append(")");
						}
					}
					sb.append(", ");
					troubleAxis = (axisNumber1 >= dataSource.numDimensions() || dataSource.getAxisType(axisNumber1) == null);
					sb.append( troubleAxis ? "d1" : dataSource.getAxisType(axisNumber1));
					sb.append("= ");
					sb.append(dataV);
					// only display calibrated values if they are not == 1.0 * uncalibrated values
					if (axisNumber1 < dataSource.numDimensions()) {
						if (realWorldCoords[axisNumber1].subtract(BigDecimal.valueOf(modelCoords[axisNumber1])).abs().compareTo(BigDecimal.valueOf(0.000001)) > 0) {
							sb.append(" (");
							sb.append(df.format(realWorldCoords[axisNumber1]));
							sb.append(" ");
							sb.append(dataSource.getAxisUnit(axisNumber1) == null ? "" : dataSource.getAxisUnit(axisNumber1));
							sb.append(")");
						}
					}
					sb.append(", value = ");
					if (alternateValue != null)
						sb.append(alternateValue);
					else
						sb.append(hpVal);
					sb.append(" ");
					sb.append(dataSource.getValueUnit() == null ? "" : dataSource.getValueUnit());
					readout.setText(sb.toString());
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
			}
		});
		
		JPanel zoomPanel = new JPanel();
		BoxLayout buttonBoxLayoutZ = new BoxLayout(zoomPanel, BoxLayout.Y_AXIS);
		JLabel scaleLabel = new JLabel(""+scale+"X");
		JButton incZoom = new JButton("Zoom In");
		JButton decZoom = new JButton("Zoom Out");
		JButton resetZoom = new JButton("Reset");
		incZoom.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (scale < Math.min(view.d0(), view.d1())) {
					scale++;
					scaleLabel.setText(""+scale+"X");
					draw();
					//frame.invalidate();
					frame.repaint();
				}
			}
		});
		decZoom.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (scale > 1) {
					scale--;
					scaleLabel.setText(""+scale+"X");
					draw();
					frame.repaint();
				}
			}
		});
		resetZoom.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				scale = 1;
				scaleLabel.setText(""+scale+"X");
				draw();
				frame.repaint();
			}
		});
		zoomPanel.setLayout(buttonBoxLayoutZ);
		zoomPanel.add(new JLabel("Scale:"));
		zoomPanel.add(scaleLabel);
		zoomPanel.add(incZoom);
		zoomPanel.add(decZoom);
		zoomPanel.add(resetZoom);

		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new BorderLayout());
		sliderPanel.add(readout, BorderLayout.NORTH);
		sliderPanel.add(positions, BorderLayout.CENTER);

		frame.add(graphicsPanel, BorderLayout.CENTER);
		frame.add(headerPanel, BorderLayout.NORTH);
		frame.add(sliderPanel, BorderLayout.SOUTH);
		frame.add(buttonPanel, BorderLayout.EAST);
		frame.add(zoomPanel, BorderLayout.WEST);
		
		frame.pack();

		frame.setVisible(true);

		calcMinMax();
		
		draw();
		
		frame.repaint();
	}
	
	/**
	 * Assigns a new color table through which the viewer displays plane data.
	 * 
	 * @param colorTable The ramp of argb colors.
	 */
	public void setColorTable(int[] colorTable) {
		this.colorTable = colorTable;
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
	
	// calcs the pixel type's value bounds
	
	private <V extends Algebra<V,U> & Bounded<U>>
		void pixelTypeBounds(T alg, U min, U max)
	{
		if (alg instanceof Bounded) {
			@SuppressWarnings("unchecked")
			V enhancedAlg = (V) alg;
			enhancedAlg.minBound().call(min);
			enhancedAlg.maxBound().call(max);
		}
		else {
			alg.zero().call(min);
			alg.zero().call(max);
		}
	}
	
	// calcs the data source's actual value bounds
	
	private <V extends Algebra<V,U> & Ordered<U>>
		void pixelDataBounds(T alg, IndexedDataSource<U> data, U min, U max)
	{
		if (alg instanceof Ordered) {
			@SuppressWarnings("unchecked")
			V enhancedAlg = (V) alg;
			MinMaxElement.compute(enhancedAlg, data, min, max);
		}
		else {
			alg.zero().call(min);
			alg.zero().call(max);
		}
	}
	
	// calc the display range by either data bounds or type bounds
	
	private void calcMinMax() {

		if (preferDataRange) {
			pixelDataBounds(alg, view.getDataSource().rawData(), min, max);
			if (alg.isEqual().call(min, max))
				pixelTypeBounds(alg, min, max);
		}
		else {
			pixelTypeBounds(alg, min, max);
			if (alg.isEqual().call(min, max))
				pixelDataBounds(alg, view.getDataSource().rawData(), min, max);
		}
	}
	
	// draw the data

	private void draw() {

		U value = alg.construct();
		
		HighPrecRepresentation valHi = null;
		if (value instanceof HighPrecRepresentation) {
			valHi = (HighPrecRepresentation) value;
		}
		
		// Safe cast as img is of correct type 
		
		DataBufferInt buffer = (DataBufferInt) argbData.getRaster().getDataBuffer();

		// Conveniently, the buffer already contains the data array
		int[] arrayInt = buffer.getData();

		HighPrecisionMember hpPix = new HighPrecisionMember();
		HighPrecisionMember hpMin;
		HighPrecisionMember hpMax;
		if (valHi != null) {
			hpMin = new HighPrecisionMember();
			hpMax = new HighPrecisionMember();
			((HighPrecRepresentation) min).toHighPrec(hpMin);
			((HighPrecRepresentation) max).toHighPrec(hpMax);
		}
		else {
			throw new IllegalArgumentException("this algo requires high precision support from type");
		}

		for (int model1 = 0; model1 < view.d1() / scale; model1++) {
			for (int model0 = 0; model0 < view.d0() / scale; model0++) {

				view.get(model0, model1, value);
		
				// scale the current value to an intensity from 0 to 1.
				//   Note that HP values can't represent NaNs and Infs so we must handle

				BigDecimal numer;
				BigDecimal denom;

				if ((nanTester != null) && nanTester.isNaN().call(value)) {
					numer = BigDecimal.ZERO;
					denom = BigDecimal.ONE;
				}
				else if ((infTester != null) && infTester.isInfinite().call(value)) {
					
					if (signumTester.signum().call(value) <= 0)
						numer = BigDecimal.ZERO;
					else
						numer = BigDecimal.ONE;
					denom = BigDecimal.ONE;
				}
				else {
					
					valHi.toHighPrec(hpPix);
					numer = hpPix.v().subtract(hpMin.v());
					denom = hpMax.v().subtract(hpMin.v());
				}
				
				// image with zero display range
				if (denom.compareTo(BigDecimal.ZERO) == 0)
					denom = BigDecimal.ONE;
				
				BigDecimal ratio = numer.divide(denom, HighPrecisionAlgebra.getContext());
				
				if (ratio.compareTo(BigDecimal.ZERO) < 0)
					ratio = BigDecimal.ZERO;

				if (ratio.compareTo(BigDecimal.ONE) > 0)
					ratio = BigDecimal.ONE;
				
				// now scale 0-1 to the range of the size of the current color table
				
				BigDecimal colorTableSize = BigDecimal.valueOf(colorTable.length-1);
				BigDecimal colorTableIndex = colorTableSize.multiply(ratio);
				colorTableIndex = colorTableIndex.add(BigDecimalUtils.ONE_HALF);  // force HALF EVEN rounding
				int intensity = colorTableIndex.intValue();

				// put a color from the color table into the image at (pssibly many) correct positions

				int view0 = model0 * scale;
				int view1 = model1 * scale;
				
				for (int dy = 0; dy < scale; dy++) {
					
					if ((view1 + dy) > view.d1()) continue;
						
					for (int dx = 0; dx < scale; dx++) {

						if ((view0 + dx) > view.d0()) continue;

						int bufferPos = (view1 + dy) * view.d0() + (view0 + dx);
						
						arrayInt[bufferPos] = colorTable[intensity];
					}
				}
			}
		}
	}
}
