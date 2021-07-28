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
import java.awt.Font;
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
import nom.bdezonia.zorbage.misc.BigDecimalUtils;
import nom.bdezonia.zorbage.type.color.RgbUtils;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionAlgebra;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionMember;

// TODO
// - zoom out not working
// - look and feel still quite bad
// - should snapshot respect zoom?
// - pan down and pan left not respecting boundaries (for boats)

/**
 * 
 * @author Barry DeZonia
 *
 */
public class RealImageViewer<T extends Algebra<T,U>, U> {

	private final T alg;
	private final PlaneView<U> planeData;
	private final PanZoomView pz;
	private final BufferedImage argbData;
	private int[] colorTable = LutUtils.DEFAULT_COLOR_TABLE;
	private boolean preferDataRange = true;
	private final U min;
	private final U max;
	private final JLabel[] positionLabels;
	private final JFrame frame;
	private NaN<U> nanTester = null;
	private Infinite<U> infTester = null;
	private Ordered<U> signumTester = null;
	private int axisNumber0, axisNumber1;
	private final Font font = new Font("Verdana", Font.PLAIN, 18);

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
	@SuppressWarnings("unchecked")
	public RealImageViewer(T alg, DimensionedDataSource<U> dataSource, int axisNumber0, int axisNumber1) {

		this.alg = alg;
		this.planeData = new PlaneView<>(dataSource, axisNumber0, axisNumber1);
		this.pz = new PanZoomView(512, 512);
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
	
		// temperature, pressure, speed, etc
		
		String valueInfo = "(unknown family)";
		if (dataType != null)
			valueInfo = dataType;
	
		// degrees K, mHg, km/h, etc
		
		String valueUnit = "(unknown unit)";
		if (dataUnit != null)
			valueUnit = " (" + dataUnit + ")";
		
		frame = new JFrame(title);
		
		frame.setLayout(new BorderLayout());
		
		argbData = new BufferedImage(pz.paneWidth, pz.paneHeight, BufferedImage.TYPE_INT_ARGB);
		
		positionLabels = new JLabel[planeData.getPositionsCount()];
		for (int i = 0; i < positionLabels.length; i++) {
			positionLabels[i] = new JLabel();
			positionLabels[i].setFont(font);
		}
		
		JLabel sourceLabel = new JLabel("Source: "+source);
		sourceLabel.setFont(font);
		sourceLabel.setOpaque(true);

		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.add(sourceLabel);
		JLabel a = new JLabel("Data type: " + alg.typeDescription());
		JLabel b = new JLabel("Data family: " + valueInfo);
		JLabel c = new JLabel("Data unit: " + valueUnit);
		a.setFont(font);
		b.setFont(font);
		c.setFont(font);
		headerPanel.add(a);
		headerPanel.add(b);
		headerPanel.add(c);
		
		JPanel graphicsPanel = new JPanel();
		JLabel image = new JLabel(new ImageIcon(argbData));
		JScrollPane scrollPane = new JScrollPane(image);
		graphicsPanel.add(scrollPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		BoxLayout buttonBoxLayout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
		buttonPanel.setLayout(buttonBoxLayout);
		JButton loadLut = new JButton("Load LUT");
		JButton resetLut = new JButton("Reset LUT");
		JButton newView = new JButton("New View");
		JButton snapshot = new JButton("Snapshot");
		JButton incZoom = new JButton("Zoom In");
		JButton decZoom = new JButton("Zoom Out");
		JButton panLeft = new JButton("Left");
		JButton panRight = new JButton("Right");
		JButton panUp = new JButton("Up");
		JButton panDown = new JButton("Down");
		JButton resetZoom = new JButton("Reset");
		JCheckBox check = new JCheckBox("Use data range");
		check.setSelected(preferDataRange);
		check.setFont(font);
		buttonPanel.add(loadLut);
		buttonPanel.add(resetLut);
		buttonPanel.add(newView);
		buttonPanel.add(snapshot);
		buttonPanel.add(incZoom);
		buttonPanel.add(decZoom);
		buttonPanel.add(panLeft);
		buttonPanel.add(panRight);
		buttonPanel.add(panUp);
		buttonPanel.add(panDown);
		buttonPanel.add(resetZoom);
		buttonPanel.add(new JSeparator());
		JLabel scaleLabel = new JLabel("Scale: " + pz.effectiveScale());
		scaleLabel.setFont(font);
		buttonPanel.add(scaleLabel);
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

				// TODO : restore me
				//
				//DimensionedDataSource<U> snap = view.takeSnapsot(alg.construct());
				//
				//new RealImageViewer<>(alg, snap);
			}
		});
		incZoom.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				pz.increaseZoom();
				scaleLabel.setText("Scale: " + pz.effectiveScale());
				pz.draw();
				frame.repaint();
			}
		});
		decZoom.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				pz.decreaseZoom();
				scaleLabel.setText("Scale: " + pz.effectiveScale());
				pz.draw();
				frame.repaint();
			}
		});
		panLeft.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				pz.panLeft(75);
				pz.draw();
				frame.repaint();
			}
		});
		panRight.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				pz.panRight(75);
				pz.draw();
				frame.repaint();
			}
		});
		panUp.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				pz.panUp(75);
				pz.draw();
				frame.repaint();
			}
		});
		panDown.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				pz.panDown(75);
				pz.draw();
				frame.repaint();
			}
		});
		resetZoom.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				pz.reset();  // no diff between pan reset and zoom reset. should there be?
				scaleLabel.setText("Scale: " + pz.effectiveScale());
				pz.draw();
				frame.repaint();
			}
		});
		
		JPanel positions = new JPanel();
		BoxLayout positionsBoxLayout = new BoxLayout(positions, BoxLayout.Y_AXIS);
		positions.setLayout(positionsBoxLayout);
		
		for (int i = 0; i < planeData.getPositionsCount(); i++) {
			JPanel miniPanel = new JPanel();
			miniPanel.setLayout(new FlowLayout());
			JButton homeButton = new JButton("<<");
			JButton decrementButton = new JButton("<");
			JButton incrementButton = new JButton(">");
			JButton endButton = new JButton(">>");
			long maxVal = planeData.getDataSourceAxisSize(i);
			positionLabels[i].setText(""+(planeData.getPositionValue(i)+1)+" / "+maxVal);
			int pos = planeData.getDataSourceAxisNumber(i);
			String axisLabel;
			if (planeData.getDataSource().getAxisType(pos) == null)
				axisLabel = "" + pos + " : ";
			else
				axisLabel = planeData.getDataSource().getAxisType(pos) + " : ";
			JLabel jax = new JLabel(axisLabel);
			jax.setFont(font);
			miniPanel.add(jax);
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
		readout.setFont(font);
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
				long i0 = pz.pixelToModel(e.getX(), pz.getVirtualOriginX());
				long i1 = pz.pixelToModel(e.getY(), pz.getVirtualOriginY());
				if (i0 >= 0 && i0 < planeData.d0() && i1 >= 0 && i1 < planeData.d1()) {
					planeData.getModelCoords(i0, i1, modelCoords);
					dataSource.getCoordinateSpace().project(modelCoords, realWorldCoords);
					planeData.get(i0, i1, value);
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
					int axisNumber0 = planeData.axisNumber0();
					int axisNumber1 = planeData.axisNumber1();
					StringBuilder sb = new StringBuilder();
					troubleAxis = (axisNumber0 >= dataSource.numDimensions() || dataSource.getAxisType(axisNumber0) == null);
					sb.append(troubleAxis ? "d0" : dataSource.getAxisType(axisNumber0));
					sb.append(" = ");
					sb.append(i0);
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
					sb.append(i1);
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
		
		JPanel miscPanel = new JPanel();
		BoxLayout miscBoxLayout = new BoxLayout(miscPanel, BoxLayout.Y_AXIS);
		miscPanel.setLayout(miscBoxLayout);
		miscPanel.add(new JSeparator());
		JLabel d = new JLabel("Dimensions");
		d.setFont(font);
		miscPanel.add(d);
		for (int i = 0; i < dataSource.numDimensions(); i++) {
			String axisName = dataSource.getAxisType(i);
			if (axisName == null)
				axisName = "d" + i;
			JLabel dimLabel = new JLabel(dataSource.dimension(i)+" : "+axisName);
			dimLabel.setFont(font);
			miscPanel.add(dimLabel);
		}
		miscPanel.add(check);
		miscPanel.add(new JSeparator());
		check.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				preferDataRange = !preferDataRange;
				calcMinMax();
				pz.draw();
				frame.repaint();
			}
		});

		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new BorderLayout());
		sliderPanel.add(readout, BorderLayout.NORTH);
		sliderPanel.add(positions, BorderLayout.CENTER);

		frame.add(graphicsPanel, BorderLayout.CENTER);
		frame.add(headerPanel, BorderLayout.NORTH);
		frame.add(sliderPanel, BorderLayout.SOUTH);
		frame.add(buttonPanel, BorderLayout.EAST);
		frame.add(miscPanel, BorderLayout.WEST);
		
		frame.pack();

		frame.setVisible(true);

		calcMinMax();
		
		pz.draw();
		
		frame.repaint();
	}
	
	/**
	 * Assigns a new color table through which the viewer displays plane data.
	 * 
	 * @param colorTable The ramp of argb colors.
	 */
	public void setColorTable(int[] colorTable) {
		this.colorTable = colorTable;
		pz.draw();
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
			long maxVal = planeData.getDataSourceAxisSize(extraPos);
			long pos = planeData.getPositionValue(extraPos);
			if (pos < maxVal - 1) {
				pos++;
				planeData.setPositionValue(extraPos, pos);
				positionLabels[extraPos].setText(""+(pos+1)+" / "+maxVal);
				pz.draw();
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
			long maxVal = planeData.getDataSourceAxisSize(extraPos);
			long pos = planeData.getPositionValue(extraPos);
			if (pos > 0) {
				pos--;
				planeData.setPositionValue(extraPos, pos);
				positionLabels[extraPos].setText(""+(pos+1)+" / "+maxVal);
				pz.draw();
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
			long maxVal = planeData.getDataSourceAxisSize(extraPos);
			planeData.setPositionValue(extraPos, 0);
			positionLabels[extraPos].setText(""+(1)+" / "+maxVal);
			pz.draw();
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
			long maxVal = planeData.getDataSourceAxisSize(extraPos);
			planeData.setPositionValue(extraPos, maxVal-1);
			positionLabels[extraPos].setText(""+(maxVal)+" / "+maxVal);
			pz.draw();
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
			pixelDataBounds(alg, planeData.getDataSource().rawData(), min, max);
			if (alg.isEqual().call(min, max))
				pixelTypeBounds(alg, min, max);
		}
		else {
			pixelTypeBounds(alg, min, max);
			if (alg.isEqual().call(min, max))
				pixelDataBounds(alg, planeData.getDataSource().rawData(), min, max);
		}
	}
	
	@SuppressWarnings("unchecked")
	class PanZoomView {
		
		private final BigDecimal NAN_CODE = BigDecimal.valueOf(-100);
		private final BigDecimal POSINF_CODE = BigDecimal.valueOf(-200);
		private final BigDecimal NEGINF_CODE = BigDecimal.valueOf(-300);

		private int scaleNumer; // >= 1
		private int scaleDenom; // >= 1
		private long originX;  // model coords
		private long originY;  // model coords
		private final int paneWidth; // pixel window coords
		private final int paneHeight;  // pixel window coords
		private long calculatedPaneWidth; // the best guess at model width of paneWidth at curr scale/offset
		private long calculatedPaneHeight; // the best guess at model height of paneHeight at curr scale/offset
		private final int maxScale;
		NaN<U> nanTester = null;
		Infinite<U> infTester = null;
		Ordered<U> signumTester = null;
		
		public PanZoomView(int paneWidth, int paneHeight) {
			this.paneWidth = paneWidth;
			this.paneHeight = paneHeight;
			this.maxScale = Math.min(paneWidth, paneHeight);
			setInitialNumbers();
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
			if (!(alg.construct() instanceof HighPrecRepresentation)) {
				throw new IllegalArgumentException(
						"this viewer requires the real image to support HighPrecisionRepresentation");
			}
		}

		private void setInitialNumbers() {
			this.calculatedPaneWidth = paneWidth;
			this.calculatedPaneHeight = paneHeight;
			this.scaleNumer = 1;
			this.scaleDenom = 1;
			long modelWidth = planeData.d0();
			long modelHeight = planeData.d1();
			long ctrX = modelWidth / 2;
			long ctrY = modelHeight / 2;
			long ctrViewX = paneWidth / 2;
			long ctrViewY = paneHeight / 2;
			this.originX = ctrX - ctrViewX;
			this.originY = ctrY - ctrViewY;;
		}
		
		private void calcPaneSize() {
			if (scaleNumer == 1 && scaleDenom == 1) {
				this.calculatedPaneWidth = paneWidth;
				this.calculatedPaneHeight = paneHeight;
			}
			else if (scaleNumer > 1) {
				this.calculatedPaneWidth = paneWidth / scaleNumer;
				this.calculatedPaneHeight = paneHeight / scaleNumer;
			}
			else if (scaleDenom > 1) {
				this.calculatedPaneWidth = ((long) paneWidth) * scaleDenom;
				this.calculatedPaneHeight = ((long) paneHeight) * scaleDenom;
			}
			else
				throw new IllegalArgumentException("weird scale components "+scaleNumer+" "+scaleDenom);
		}

		public long getVirtualOriginX() {
			return originX;
		}
		
		public long getVirtualOriginY() {
			return originY;
		}
		
		public long getVirtualWidth() {
			return calculatedPaneWidth;
		}
		
		public long getVirtualHeight() {
			return calculatedPaneHeight;
		}
		
		public int drawingBoxHalfSize() {  // this works well when we only support odd zoom factors
			return scaleNumer / 2;
		}
		
		public int intensityBoxHalfSize() {
			return scaleDenom / 2;  // this works well when we only support odd zoom factors
		}
		
		public void setScaleVars(int numer, int denom) {
			if (numer == 1) {
				if (denom < 1)
					throw new IllegalArgumentException("illegal scale denominator");
			}
			else if (denom == 1) {
				if (numer < 1)
					throw new IllegalArgumentException("illegal scale numerator");
			}
			else
				throw new IllegalArgumentException("unsupported scale combo; either numer or denom must be 1");
			scaleNumer = numer;
			scaleDenom = denom;
			calcPaneSize();
		}

		public void reset() {
			setInitialNumbers();
		}
		
		public void increaseZoom() {
			
			boolean changed = false;

			if (scaleDenom >= 3) {
				int origScale = scaleDenom;
				int newScale = scaleDenom - 2;
				long origXExtent = getVirtualWidth();
				long origYExtent = getVirtualHeight();
				long newXExtent = origXExtent * newScale / origScale;
				long newYExtent = origYExtent * newScale / origScale;
				long modelChangeForOriginX = Math.abs(origXExtent - newXExtent) / 2;
				long modelChangeForOriginY = Math.abs(origYExtent - newYExtent) / 2;
				originX += modelChangeForOriginX;
				originY += modelChangeForOriginY;
				scaleDenom = newScale;
				changed = true;
			}
			else if (scaleNumer + 2 <= maxScale) {
				int origScale = scaleNumer;
				int newScale = scaleNumer + 2;
				long origXExtent = getVirtualWidth();
				long origYExtent = getVirtualHeight();
				long newXExtent = origXExtent * origScale / newScale;
				long newYExtent = origYExtent * origScale / newScale;
				long modelChangeForOriginX = Math.abs(origXExtent - newXExtent) / 2;
				long modelChangeForOriginY = Math.abs(origYExtent - newYExtent) / 2;
				originX += modelChangeForOriginX;
				originY += modelChangeForOriginY;
				scaleNumer = newScale;
				changed = true;
			}

			if (changed) calcPaneSize();
		}
		
		
		public void decreaseZoom() {
			
			boolean changed = false;
			
			if (scaleNumer >= 3) {
				int origScale = scaleNumer;
				int newScale = scaleNumer - 2;
				long origXExtent = getVirtualWidth();
				long origYExtent = getVirtualHeight();
				long newXExtent = origXExtent * origScale / newScale;
				long newYExtent = origYExtent * origScale / newScale;
				long modelChangeForOriginX = Math.abs(origXExtent - newXExtent) / 2;
				long modelChangeForOriginY = Math.abs(origYExtent - newYExtent) / 2;
				originX -= modelChangeForOriginX;
				originY -= modelChangeForOriginY;
				scaleNumer = newScale;
				changed = true;
			}
			else if (scaleDenom + 2 <= maxScale) {
				int origScale = scaleDenom;
				int newScale = scaleDenom + 2;
				long origXExtent = getVirtualWidth();
				long origYExtent = getVirtualHeight();
				long newXExtent = origXExtent * newScale / origScale;
				long newYExtent = origYExtent * newScale / origScale;
				long modelChangeForOriginX = Math.abs(origXExtent - newXExtent) / 2;
				long modelChangeForOriginY = Math.abs(origYExtent - newYExtent) / 2;
				originX -= modelChangeForOriginX;
				originY -= modelChangeForOriginY;
				scaleDenom = newScale;
				changed = true;
			}

			if (changed) calcPaneSize();
		}

		public void panLeft(int numPixels) {
			long numModelUnits = pixelToModel(numPixels, 0);
			long dim = 0;
			if (axisNumber0 < planeData.getDataSource().numDimensions())
				dim = planeData.getDataSource().dimension(axisNumber0);
			if ((originX - numModelUnits) > -dim)
				originX -= numModelUnits;
		}

		public void panRight(int numPixels) {
			long numModelUnits = pixelToModel(numPixels, 0);
			long dim = 0;
			if (axisNumber0 < planeData.getDataSource().numDimensions())
				dim = planeData.getDataSource().dimension(axisNumber0);
			if ((originX + numModelUnits) < dim)
				originX += numModelUnits;
		}

		public void panUp(int numPixels) {
			long numModelUnits = pixelToModel(numPixels, 0);
			long dim = 0;
			if (axisNumber1 < planeData.getDataSource().numDimensions())
				dim = planeData.getDataSource().dimension(axisNumber1);
			if ((originY - numModelUnits) > -dim)
				originY -= numModelUnits;
		}

		public void panDown(int numPixels) {
			long numModelUnits = pixelToModel(numPixels, 0);
			long dim = 0;
			if (axisNumber1 < planeData.getDataSource().numDimensions())
				dim = planeData.getDataSource().dimension(axisNumber1);
			if ((originY + numModelUnits) < dim)
				originY += numModelUnits;
		}
		
		public String effectiveScale() {
			if (scaleDenom == 1)
				return "" + scaleNumer + "X";
			else
				return "1/" + scaleDenom + "X";
		}
		
		private long pixelToModel(int pixelNum, long modelOffset) {
			if (scaleNumer == 1 && scaleDenom == 1) {
				return pixelNum + modelOffset;
			}
			else if (scaleNumer > 1) {
				return (((long) pixelNum) / scaleNumer) + modelOffset;
			}
			else if (scaleDenom > 1) {
				return (((long) pixelNum) * scaleDenom) + modelOffset;
			}
			else
				throw new IllegalArgumentException("back to the drawing board");
		}
		
		public void draw() {

			if (alg instanceof NaN) {
				this.nanTester = (NaN<U>) alg;
			}
			if (alg instanceof Infinite) {
				this.infTester = (Infinite<U>) alg;
			}
			if (alg instanceof Ordered) {
				this.signumTester = (Ordered<U>) alg;
			}

			// Safe cast as img is of correct type 
			
			DataBufferInt buffer = (DataBufferInt) argbData.getRaster().getDataBuffer();

			// Conveniently, the buffer already contains the data array
			int[] arrayInt = buffer.getData();
			
			HighPrecisionMember sum = G.HP.construct();
			HighPrecisionMember tmp = G.HP.construct();
			U value = alg.construct();
			DimensionedDataSource<U> data = planeData.getDataSource();
			long maxDimX = axisNumber0 < data.numDimensions() ? data.dimension(axisNumber0) : 1;
			long maxDimY = axisNumber1 < data.numDimensions() ? data.dimension(axisNumber1) : 1;
			for (int y = 0; y < paneHeight; y++) {
				for (int x = 0; x < paneWidth; x++) {
					G.HP.zero().call(sum);
					boolean includesNans = false; 
					boolean includesPosInfs = false; 
					boolean includesNegInfs = false; 
					long numCounted = 0;
					boolean modelCoordsInBounds = false;
					for (int dy = -intensityBoxHalfSize(); dy <= intensityBoxHalfSize(); dy++) {
						for (int dx = -intensityBoxHalfSize(); dx <= intensityBoxHalfSize(); dx++) {
							long mx = pixelToModel(x+dx, originX);
							long my = pixelToModel(y+dy, originY);
							if (mx >= 0 && mx < maxDimX && my >= 0 && my < maxDimY) {
								modelCoordsInBounds = true;
								planeData.get(mx, my, value);
								if (nanTester != null && nanTester.isNaN().call(value))
									includesNans = true;
								else if (infTester != null && infTester.isInfinite().call(value)) {
									if (signumTester.signum().call(value) < 0)
										includesNegInfs = true;
									else
										includesPosInfs = true;
								}
								else {
									((HighPrecRepresentation) value).toHighPrec(tmp);
									G.HP.add().call(sum, tmp, sum);
									numCounted++;
								}
							}
							int argb = 0;
							if (modelCoordsInBounds) {

								// calc average intensity

								BigDecimal avgIntensity = getIntensity(sum, numCounted, includesNans, includesPosInfs, includesNegInfs);
								
								argb = getColor(avgIntensity);
							}
							else {
								
								argb = RgbUtils.argb(255, 0, 0, 0);
							}
							
							for (int dv = -drawingBoxHalfSize(); dv <= drawingBoxHalfSize(); dv++) {
								for (int du = -drawingBoxHalfSize(); du <= drawingBoxHalfSize(); du++) {

									// plot a point
									plot(argb, arrayInt, x+du, y+dv);
								}
							}
						}
					}
					
				}
			}
		}

		private BigDecimal getIntensity(HighPrecisionMember valueSum, long numValues, boolean includesNans, boolean includesPosInfs, boolean includesNegInfs) {

			// scale the current value sum to an average intensity from 0 to 1.
			//   Note that HP values can't represent NaNs and Infs so we must handle
			//   in some other fashion.

			if (includesNans) {
				
				// Nans dominate all values: sum would be nan. We will treat as NAN_CODE
				// and getColor will choose black regardless of color table.
				
				return NAN_CODE;
			}
			else if (includesPosInfs && includesNegInfs) {
				
				// this sum would be nan too
				
				return NAN_CODE;
			}
			else if (includesPosInfs) {
				
				// this sum would be pos inf. Encode and get intensity will return white
				
				return POSINF_CODE;
			}
			else if (includesNegInfs) {
				
				// this sum would be neg inf. Encode and get intensity will return black

				return NEGINF_CODE;
			}
			else if (numValues == 0) {
				
				// this sum would be nan too
				
				return NAN_CODE;
			}
			
			HighPrecisionMember hpMin = new HighPrecisionMember();
			HighPrecisionMember hpMax = new HighPrecisionMember();
			((HighPrecRepresentation) min).toHighPrec(hpMin);
			((HighPrecRepresentation) max).toHighPrec(hpMax);

			BigDecimal average =
					valueSum.v().divide(BigDecimal.valueOf(numValues), HighPrecisionAlgebra.getContext());
			
			BigDecimal numer = average.subtract(hpMin.v());
			
			BigDecimal denom = hpMax.v().subtract(hpMin.v());
			
			BigDecimal ratio = numer.divide(denom, HighPrecisionAlgebra.getContext());

			if (ratio.compareTo(BigDecimal.ZERO) < 0)
				return BigDecimal.ZERO;
			else if (ratio.compareTo(BigDecimal.ONE) > 0)
				return BigDecimal.ONE;
			else
				return ratio;
		}
		
		// intensity is 0 <= intensity <= 1
		
		private int getColor(BigDecimal intensity) {
			
			if (intensity.compareTo(BigDecimal.ZERO) < 0) {
			
				if (intensity.compareTo(NAN_CODE) == 0) {
				
					return RgbUtils.argb(255,0,0,0);  // black
				}
				
				if (intensity.compareTo(NEGINF_CODE) == 0) {
					
					return RgbUtils.argb(255,0,0,0); // black
				}
				
				if (intensity.compareTo(POSINF_CODE) == 0) {
					
					return RgbUtils.argb(255,255,255,255);  // white
				}
			}

			// scale 0-1 to the range of the size of the current color table
			
			BigDecimal colorTableSize = BigDecimal.valueOf(colorTable.length-1);
			
			BigDecimal colorTableIndex = colorTableSize.multiply(intensity);
			
			// force correct rounding
			
			colorTableIndex = colorTableIndex.add(BigDecimalUtils.ONE_HALF);
			
			return colorTable[colorTableIndex.intValue()];
		}
		
		private void plot(int argb, int[] arrayInt, int x, int y) {
			
			if (x < 0 || x >= paneWidth || y < 0 || y >= paneHeight)
				return;
			
			int bufferPos = y * paneWidth + x;
			
			arrayInt[bufferPos] = argb;
		}
		
		/*
		 * If zoom is 4x
		 *   numer = 4
		 *   denom = 1
		 *   model section to display is 1/4 of the existing pane's size'
		 *   each pixel is drawn in a 4x4 box.
		 *   
		 * If zoom is 1/5x
		 *   numer = 1
		 *   denom = 5
		 *   model section to display is 5x of the existing pane's size'
		 *   each pixel 5x5 group of model pixels is drawn as a single pixel
		 *     (using pixel averaging?).
		 * 
		 * For displaying pixels outside model space just draw them as Black.
		 *   Or maybe draw a border to delineate the bounds.
		 * 
		 * We display origin-halfWidth to origin=halfWidth
		 */
	}
}
