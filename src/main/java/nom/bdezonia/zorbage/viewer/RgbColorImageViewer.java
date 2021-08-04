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
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;

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
import nom.bdezonia.zorbage.algebra.Allocatable;
import nom.bdezonia.zorbage.coordinates.CoordinateSpace;
import nom.bdezonia.zorbage.coordinates.LinearNdCoordinateSpace;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.data.DimensionedStorage;
import nom.bdezonia.zorbage.dataview.PlaneView;
import nom.bdezonia.zorbage.dataview.TwoDView;
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
	private final PlaneView<U> planeData;
	private final PanZoomView pz;
	private final BufferedImage argbData;
	private final JLabel[] positionLabels;
	private final JFrame frame;
	private final Font font = new Font("Verdana", Font.PLAIN, 18);
	private final AtomicBoolean animatingRightNow = new AtomicBoolean();
	private final JLabel ctrXLabel; 
	private final JLabel ctrYLabel; 
	DecimalFormat df = new DecimalFormat("0.000");

	/**
	 * Make an interactive graphical viewer for a real data source.
	 * @param alg The algebra that matches the type of data to display
	 * @param dataSource The data to display
	 */
	public RgbColorImageViewer(T alg, DimensionedDataSource<U> dataSource) {
		this(alg, dataSource, 0, 1);
	}

	/**
	 * Make an interactive graphical viewer for a real data source.
	 * @param alg The algebra that matches the type of data to display
	 * @param dataSource The data to display
	 * @param axisNumber0 The first axis number defining the planes to view (x, y, z, c, t, etc.)
	 * @param axisNumber1 The second axis number defining the planes to view (x, y, z, c, t, etc.)
	 */
	public RgbColorImageViewer(T alg, DimensionedDataSource<U> dataSource, int axisNumber0, int axisNumber1) {

		this.alg = alg;
		this.planeData = new PlaneView<>(dataSource, axisNumber0, axisNumber1);
		this.pz = new PanZoomView(512, 512);

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
		if (dataType != null && dataType.length() != 0)
			valueInfo = dataType;
	
		// degrees K, mHg, km/h, etc
		
		String valueUnit = "(unknown unit)";
		if (dataUnit != null && dataUnit.length() != 0)
			valueUnit = " (" + dataUnit + ")";
		
		frame = new JFrame(title);
		
		frame.setLayout(new BorderLayout());
		
		argbData = new BufferedImage(pz.paneWidth, pz.paneHeight, BufferedImage.TYPE_INT_ARGB);
		
		positionLabels = new JLabel[planeData.getPositionsCount()];
		for (int i = 0; i < positionLabels.length; i++) {
			positionLabels[i] = new JLabel();
			positionLabels[i].setFont(font);
		}
		
		JLabel scaleLabel = new JLabel("Scale: 1X");
		scaleLabel.setFont(font);

		ctrXLabel = new JLabel("Zoom center d0:");
		ctrXLabel.setFont(font);

		ctrYLabel = new JLabel("Zoom center d1:");
		ctrYLabel.setFont(font);

		setZoomCenterLabels();
		
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
		JButton swapAxes = new JButton("Swap Axes");
		JButton snapshot = new JButton("1X Snapshot");
		JButton incZoom = new JButton("Zoom In");
		JButton decZoom = new JButton("Zoom Out");
		JButton panLeft = new JButton("Left");
		JButton panRight = new JButton("Right");
		JButton panUp = new JButton("Up");
		JButton panDown = new JButton("Down");
		JButton resetZoom = new JButton("Reset");
		buttonPanel.add(swapAxes);
		buttonPanel.add(snapshot);
		buttonPanel.add(incZoom);
		buttonPanel.add(decZoom);
		buttonPanel.add(panLeft);
		buttonPanel.add(panRight);
		buttonPanel.add(panUp);
		buttonPanel.add(panDown);
		buttonPanel.add(resetZoom);
		buttonPanel.add(new JSeparator());
		swapAxes.addActionListener(new ActionListener() {
			
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
		snapshot.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {

				DimensionedDataSource<U> snap = pz.takeSnapshot();

				new RgbColorImageViewer<>(alg, snap);
			}
		});
		incZoom.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pz.increaseZoom()) {
					scaleLabel.setText("Scale: " + pz.effectiveScale());
					setZoomCenterLabels();
					pz.draw();
					frame.repaint();
				}
			}
		});
		decZoom.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pz.decreaseZoom()) {
					scaleLabel.setText("Scale: " + pz.effectiveScale());
					setZoomCenterLabels();
					pz.draw();
					frame.repaint();
				}
			}
		});
		panLeft.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pz.panLeft(75)) {
					setZoomCenterLabels();
					pz.draw();
					frame.repaint();
				}
			}
		});
		panRight.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pz.panRight(75)) {
					setZoomCenterLabels();
					pz.draw();
					frame.repaint();
				}
			}
		});
		panUp.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pz.panUp(75)) {
					setZoomCenterLabels();
					pz.draw();
					frame.repaint();
				}
			}
		});
		panDown.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pz.panDown(75)) {
					setZoomCenterLabels();
					pz.draw();
					frame.repaint();
				}
			}
		});
		resetZoom.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				pz.reset();
				scaleLabel.setText("Scale: " + pz.effectiveScale());
				setZoomCenterLabels();
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
			JButton animButton = new JButton("Animate");
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
			miniPanel.add(animButton);
			positions.add(miniPanel);
			decrementButton.addActionListener(new Decrementer(i));
			incrementButton.addActionListener(new Incrementer(i));
			homeButton.addActionListener(new Home(i));
			endButton.addActionListener(new End(i));
			animButton.addActionListener(new Animator(i));
		}

		JLabel readout = new JLabel();
		readout.setBackground(Color.WHITE);
		readout.setOpaque(true);
		readout.setText("<placeholder>");
		readout.setFont(font);
		scrollPane.addMouseMotionListener(new MouseMotionListener() {

			long[] modelCoords = new long[dataSource.numDimensions()];
			BigDecimal[] realWorldCoords = new BigDecimal[dataSource.numDimensions()]; 
			U value = alg.construct();
			RgbMember rgb = (value instanceof RgbMember) ? (RgbMember) value : null;
			ArgbMember argb = (value instanceof ArgbMember) ? (ArgbMember) value : null;
					
			@Override
			public void mouseMoved(MouseEvent e) {
				boolean troubleAxis;
				long i0 = pz.pixelToModel(e.getX(), pz.getVirtualOriginX());
				long i1 = pz.pixelToModel(e.getY(), pz.getVirtualOriginY());
				if (i0 >= 0 && i0 < planeData.d0() && i1 >= 0 && i1 < planeData.d1()) {
					planeData.getModelCoords(i0, i1, modelCoords);
					dataSource.getCoordinateSpace().project(modelCoords, realWorldCoords);
					planeData.get(i0, i1, value);
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
					if (rgb != null) {
						sb.append('(');
						sb.append(rgb.r());
						sb.append(',');
						sb.append(rgb.g());
						sb.append(',');
						sb.append(rgb.b());
						sb.append(')');
						
					} else if (argb != null) {
						sb.append('(');
						sb.append(argb.a());
						sb.append(',');
						sb.append(argb.r());
						sb.append(',');
						sb.append(argb.g());
						sb.append(',');
						sb.append(argb.b());
						sb.append(')');
					}
					else
						throw new IllegalArgumentException("strange color type error");
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
		miscPanel.add(new JSeparator());
		miscPanel.add(scaleLabel);
		miscPanel.add(ctrXLabel);
		miscPanel.add(ctrYLabel);
		miscPanel.add(new JSeparator());

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
	// code to set a slider to its max value and react

	private class Animator implements ActionListener {

		private final int extraPos;

		public Animator(int extraNum) {
			extraPos = extraNum;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean wasAlreadyAnimating = animatingRightNow.getAndSet(true);
			if (wasAlreadyAnimating) return;
			long maxVal = planeData.getDataSourceAxisSize(extraPos);
			for (long i = 0; i < maxVal; i++) {
				planeData.setPositionValue(extraPos, i);
				positionLabels[extraPos].setText(""+(i+1)+" / "+maxVal);
				pz.draw();
				// paint needed instead of repaint to show immediate animation
				// But this method has a LOT of flicker. ImageJ1 uses double
				// buffered drawing to avoid flicker. See ImageCanvas paint()
				// I think.
				frame.paint(frame.getGraphics());
				try {
					Thread.sleep(100);
				} catch(InterruptedException excep) {
					;
				}
			}
			animatingRightNow.set(false);
		}
	}

	private void setZoomCenterLabels() {

		// TODO - take this code and make a viewCoord to modelCoord xform. Use it
		// in mouse listener and here to DRY code up
		
		DimensionedDataSource<?> model = planeData.getDataSource();

		long[] modelCoords = new long[model.numDimensions()];
		BigDecimal[] realWorldCoords = new BigDecimal[model.numDimensions()];

		int axisNumber0 = planeData.axisNumber0();
		int axisNumber1 = planeData.axisNumber1();

		long i0 = pz.pixelToModel(pz.paneWidth/2, pz.getVirtualOriginX());
		long i1 = pz.pixelToModel(pz.paneHeight/2, pz.getVirtualOriginY());
		
		planeData.getModelCoords(i0, i1, modelCoords);
		
		model.getCoordinateSpace().project(modelCoords, realWorldCoords);
		
		ctrXLabel.setText("Zoom Ctr d0: " + df.format(realWorldCoords[axisNumber0]));
		ctrYLabel.setText("Zoom Ctr d1: " + df.format(realWorldCoords[axisNumber1]));
	}
	
	@SuppressWarnings("unchecked")
	class PanZoomView {
		
		private int scaleNumer; // >= 1
		private int scaleDenom; // >= 1
		private long originX;  // model coords
		private long originY;  // model coords
		private final int paneWidth; // pixel window coords
		private final int paneHeight;  // pixel window coords
		private long calculatedPaneWidth; // the best guess at model width of paneWidth at curr scale/offset
		private long calculatedPaneHeight; // the best guess at model height of paneHeight at curr scale/offset
		private final int maxScale;
		
		public PanZoomView(int paneWidth, int paneHeight) {
			this.paneWidth = paneWidth;
			this.paneHeight = paneHeight;
			this.maxScale = Math.min(paneWidth, paneHeight);
			setInitialNumbers();
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
			// old way
			//return scaleDenom / 2;  // this works well when we only support odd zoom factors
			
			// new way: much much faster
			return 0;
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
		
		public boolean increaseZoom() {
			
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
			
			return changed;
		}
		
		
		public boolean decreaseZoom() {
			
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
			
			return changed;
		}

		public boolean panLeft(int numPixels) {
			long numModelUnits = pixelToModel(numPixels, 0);
			long newPos = originX - numModelUnits;
			if ((newPos <= 5 - planeData.d0()))
				return false;
			if ((newPos >= planeData.d0() - 5))
				return false;
			originX = newPos;
			return true;
		}

		public boolean panRight(int numPixels) {
			long numModelUnits = pixelToModel(numPixels, 0);
			long newPos = originX + numModelUnits;
			if ((newPos <= 5 - planeData.d0()))
				return false;
			if ((newPos >= planeData.d0() - 5))
				return false;
			originX = newPos;
			return true;
		}

		public boolean panUp(int numPixels) {
			long numModelUnits = pixelToModel(numPixels, 0);
			long newPos = originY - numModelUnits;
			if ((newPos <= 5 - planeData.d1()))
				return false;
			if ((newPos >= planeData.d1() - 5))
				return false;
			originY = newPos;
			return true;
		}

		public boolean panDown(int numPixels) {
			long numModelUnits = pixelToModel(numPixels, 0);
			long newPos = originY + numModelUnits;
			if ((newPos <= 5 - planeData.d1()))
				return false;
			if ((newPos >= planeData.d1() - 5))
				return false;
			originY = newPos;
			return true;
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
		
		private BigInteger modelToPixel(long modelNum, long modelOffset) {

			if (scaleNumer == 1 && scaleDenom == 1) {
				return BigInteger.valueOf(modelNum).subtract(BigInteger.valueOf(modelOffset));
			}
			else if (scaleNumer > 1) {
				return BigInteger.valueOf(modelNum).subtract(BigInteger.valueOf(modelOffset)).multiply(BigInteger.valueOf(scaleNumer));
			}
			else if (scaleDenom > 1) {
				return BigInteger.valueOf(modelNum).subtract(BigInteger.valueOf(modelOffset)).divide(BigInteger.valueOf(scaleDenom));
			}
			else
				throw new IllegalArgumentException("back to the drawing board");
		}
		
		public void draw() {

			U value = alg.construct();
			RgbMember rgb = (value instanceof RgbMember) ? (RgbMember) value : null;
			ArgbMember argb = (value instanceof ArgbMember) ? (ArgbMember) value : null;

			// Safe cast as img is of correct type 
			
			DataBufferInt buffer = (DataBufferInt) argbData.getRaster().getDataBuffer();

			// Conveniently, the buffer already contains the data array
			int[] arrayInt = buffer.getData();
			
			long maxDimX = planeData.d0();
			long maxDimY = planeData.d1();
			for (int y = 0; y < paneHeight; y++) {
				for (int x = 0; x < paneWidth; x++) {
					boolean modelCoordsInBounds = false;
					long mx = pixelToModel(x, originX);
					long my = pixelToModel(y, originY);
					if (mx >= 0 && mx < maxDimX && my >= 0 && my < maxDimY) {
						modelCoordsInBounds = true;
						planeData.get(mx, my, value);
					}
					int color = 0;
					if (modelCoordsInBounds) {

						if (rgb != null) {
							color = RgbUtils.argb(255, rgb.r(), rgb.g(), rgb.b());
						}
						else if (argb != null) {
							color = RgbUtils.argb(argb.a(), argb.r(), argb.g(), argb.b());
						}
						else
							throw new IllegalArgumentException("Unknown color type "+value.getClass().getSimpleName());
					}
					else {
						
						color = RgbUtils.argb(255, 0, 0, 0);
					}
					
					for (int dv = -drawingBoxHalfSize(); dv <= drawingBoxHalfSize(); dv++) {
						for (int du = -drawingBoxHalfSize(); du <= drawingBoxHalfSize(); du++) {

							// plot a point
							plot(color, arrayInt, x+du, y+dv);
						}
					}
					
				}
			}
			long maxX1 = planeData.d0()-1;
			long maxY1 = planeData.d1()-1;
			line(arrayInt, 0, 0, 0, maxY1);
			line(arrayInt, 0, maxY1, maxX1, maxY1);
			line(arrayInt, maxX1, maxY1, maxX1, 0);
			line(arrayInt, maxX1, 0, 0, 0);
		}
		
		private void plot(int argb, int[] arrayInt, int x, int y) {
			
			if (x < 0 || x >= paneWidth || y < 0 || y >= paneHeight)
				return;
			
			int bufferPos = y * paneWidth + x;
			
			arrayInt[bufferPos] = argb;
		}
		
		private void line(int[] arrayInt, long modelX0, long modelY0, long modelX1, long modelY1) {

			// yellow
			int COLOR = RgbUtils.argb(180, 0xff, 0xff, 0);
			
			BigInteger pX0 = modelToPixel(modelX0, originX);
			BigInteger pX1 = modelToPixel(modelX1, originX);
			BigInteger pY0 = modelToPixel(modelY0, originY);
			BigInteger pY1 = modelToPixel(modelY1, originY);
			
			// if the line coords are totally out of bounds then skip drawing
			
			if (pX0.compareTo(BigInteger.ZERO) < 0 &&
					pX1.compareTo(BigInteger.ZERO) < 0)
				return;
			
			if (pX0.compareTo(BigInteger.valueOf(paneWidth-1)) > 0 &&
					pX1.compareTo(BigInteger.valueOf(paneWidth-1)) > 0)
				return;
			
			if (pY0.compareTo(BigInteger.ZERO) < 0 &&
					pY1.compareTo(BigInteger.ZERO) < 0)
				return;
			
			if (pY0.compareTo(BigInteger.valueOf(paneHeight-1)) > 0 &&
					pY1.compareTo(BigInteger.valueOf(paneHeight-1)) > 0)
				return;
			
			// clip line if necessary
			
			if (pX0.compareTo(BigInteger.ZERO) < 0)
				pX0 = BigInteger.ZERO;

			if (pX1.compareTo(BigInteger.valueOf(paneWidth-1)) > 0)
				pX1 = BigInteger.valueOf(paneWidth-1);

			if (pY0.compareTo(BigInteger.ZERO) < 0)
				pY0 = BigInteger.ZERO;

			if (pY1.compareTo(BigInteger.valueOf(paneHeight-1)) > 0)
				pY1 = BigInteger.valueOf(paneHeight-1);
			
			if (pX0.compareTo(pX1) == 0) {
				int x = pX0.intValue();
				int minY = Math.min(pY0.intValue(), pY1.intValue());
				int maxY = Math.max(pY0.intValue(), pY1.intValue());
				for (int y = minY; y <= maxY; y++)
					plot(COLOR, arrayInt, x, y);
			}
			else if (pY0.compareTo(pY1) == 0) {
				int y = pY0.intValue();
				int minX = Math.min(pX0.intValue(), pX1.intValue());
				int maxX = Math.max(pX0.intValue(), pX1.intValue());
				for (int x = minX; x <= maxX; x++)
					plot(COLOR, arrayInt, x, y);
			}
			else {
				throw new IllegalArgumentException("the line() routine only deals in horz or vert lines");
			}
		}
		
		/**
		 * Make a 2d image snapshot of the pan / zoom viewport.
		 * Calibration and units are transferred to the snapshot
		 * as much as is feasible. SNapshot is taken at 1x at origin
		 * 0,0. No pan or zoom values are respected.
		 * 
		 * @return
		 */
		@SuppressWarnings("rawtypes")
		public DimensionedDataSource<U> takeSnapshot()
		{
			int axisNumber0 = planeData.axisNumber0();
			int axisNumber1 = planeData.axisNumber1();
			
			long dimX = planeData.d0();
			if (dimX > paneWidth)
				dimX = paneWidth;
			
			long dimY = planeData.d1();
			if (dimY > paneHeight)
				dimY = paneHeight;
			
			DimensionedDataSource<U> newDs = (DimensionedDataSource<U>)
					DimensionedStorage.allocate((Allocatable) alg.construct(), new long[] {dimX, dimY});
			
			U tmp = alg.construct();
			
			TwoDView<U> view = new TwoDView<>(newDs);
			
			for (int y = 0; y < dimY; y++) {
				for (int x = 0; x < dimX; x++) {
					planeData.get(x, y , tmp);
					view.set(x, y, tmp);
				}
			}
			
			DimensionedDataSource<U> origDs = planeData.getDataSource();

			String d0Str = origDs.getAxisType(axisNumber0) == null ? ("dim "+axisNumber0) : origDs.getAxisType(axisNumber0);
			String d1Str = origDs.getAxisType(axisNumber1) == null ? ("dim "+axisNumber1) : origDs.getAxisType(axisNumber1);
			String axes = "["+d0Str+":"+d1Str+"]";
			String miniTitle = axes + " : slice";

			newDs.setName(origDs.getName() == null ? miniTitle : (miniTitle + " of "+origDs.getName()));
			newDs.setAxisType(0, origDs.getAxisType(axisNumber0));
			newDs.setAxisType(1, origDs.getAxisType(axisNumber1));
			newDs.setAxisUnit(0, origDs.getAxisUnit(axisNumber0));
			newDs.setAxisUnit(1, origDs.getAxisUnit(axisNumber1));
			newDs.setValueType(origDs.getValueType());
			newDs.setValueUnit(origDs.getValueUnit());
			
			CoordinateSpace origSpace = planeData.getDataSource().getCoordinateSpace();
			if (origSpace instanceof LinearNdCoordinateSpace) {

				LinearNdCoordinateSpace origLinSpace = (LinearNdCoordinateSpace) origSpace;
				
				BigDecimal[] scales = new BigDecimal[2];

				scales[0] = origLinSpace.getScale(axisNumber0);
				scales[1] = origLinSpace.getScale(axisNumber1);
				
				BigDecimal[] offsets = new BigDecimal[2];
				
				offsets[0] = origLinSpace.getOffset(axisNumber0);
				offsets[1] = origLinSpace.getOffset(axisNumber1);
				
				long[] coord = new long[origDs.numDimensions()];
				
				coord[axisNumber0] = 0;
				coord[axisNumber1] = 0;

				offsets[0] = origLinSpace.project(coord, axisNumber0);
				offsets[1] = origLinSpace.project(coord, axisNumber1);

				LinearNdCoordinateSpace newLinSpace = new LinearNdCoordinateSpace(scales, offsets);
				
				newDs.setCoordinateSpace(newLinSpace);
			}

			return newDs;
		}
	}
}
