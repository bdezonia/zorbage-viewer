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
	private JLabel[] positionLabels;
	private final JFrame frame;
	private U oneValue;
	
	/**
	 * 
	 * @param alg
	 * @param dataSource
	 */
	public RealImageViewer(T alg, DimensionedDataSource<U> dataSource) {
		this(alg, dataSource, 0, 1);
	}

	/**
	 * 
	 * @param alg
	 * @param dataSource
	 * @param c0
	 * @param c1
	 */
	public RealImageViewer(T alg, DimensionedDataSource<U> dataSource, int c0, int c1) {

		this.alg = alg;
		this.oneValue = alg.construct();
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
			//longFields[i].setColumns(10);
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
		JButton loadLut = new JButton("Load LUT");
		JButton resetLut = new JButton("Reset LUT");
		JButton newView = new JButton("New View");
		buttonPanel.add(panLeft);
		buttonPanel.add(panRight);
		buttonPanel.add(panUp);
		buttonPanel.add(panDown);
		buttonPanel.add(loadLut);
		buttonPanel.add(resetLut);
		buttonPanel.add(newView);
		buttonPanel.add(new JSeparator());
		buttonPanel.add(new JLabel("Dimensions"));
		for (int i = 0; i < dataSource.numDimensions(); i++) {
			buttonPanel.add(new JLabel("d"+i+" : "+dataSource.dimension(i)));
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
			miniPanel.add(positionLabels[i]);
			miniPanel.add(decrementButton);
			miniPanel.add(incrementButton);
			positions.add(miniPanel);
			decrementButton.addActionListener(new Decrementer(i));
			incrementButton.addActionListener(new Incrementer(i));
		}
		
		JLabel readout = new JLabel();
		//readout.setEnabled(false);
		scrollPane.addMouseMotionListener(new MouseMotionListener() {

			HighPrecisionMember hpVal = G.HP.construct();
			
			@Override
			public void mouseMoved(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				if (x >= 0 && x < view.d0() && y >= 0 && y < view.d1()) {
					view.get(x, y, oneValue);
					HighPrecRepresentation rep = (HighPrecRepresentation) oneValue;
					rep.toHighPrec(hpVal);
					long dataX = view.origin0() + x;
					long dataY = view.origin1() + y;
					StringBuilder sb = new StringBuilder();
					sb.append("x = ");
					sb.append(dataX);
					sb.append(", y = ");
					sb.append(dataY);
					sb.append(", value = ");
					sb.append(hpVal);
					readout.setText(sb.toString());
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
			}
		});
		
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BorderLayout());
		controlPanel.add(readout, BorderLayout.NORTH);
		controlPanel.add(positions, BorderLayout.CENTER);

		frame.add(graphicsPanel, BorderLayout.CENTER);
		frame.add(buttonPanel, BorderLayout.EAST);
		frame.add(controlPanel, BorderLayout.SOUTH);
		
		frame.pack();

		frame.setVisible(true);

		draw();
		
		frame.repaint();
	}
	
	/**
	 * 
	 * @param colorTable
	 */
	public void setColorTable(int[] colorTable) {
		this.colorTable = colorTable;
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
	
	private void draw() {
		U min = alg.construct();
		U max = alg.construct();
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
		U value = alg.construct();
		boolean isHighPrec = value instanceof HighPrecRepresentation;
		
		// Safe cast as img is of correct type 
		
		DataBufferInt buffer = (DataBufferInt) argbData.getRaster().getDataBuffer();

		// Conveniently, the buffer already contains the data array
		int[] arrayInt = buffer.getData();

		HighPrecisionMember hpPix = new HighPrecisionMember();
		HighPrecisionMember hpMin;
		HighPrecisionMember hpMax;
		
		if (isHighPrec) {
			hpMin = new HighPrecisionMember();
			hpMax = new HighPrecisionMember();
			((HighPrecRepresentation) min).toHighPrec(hpMin);
			((HighPrecRepresentation) max).toHighPrec(hpMax);
		}
		else {
			// expensive but try a parser conversion if can't use highprec
			hpMin = new HighPrecisionMember(min.toString());
			hpMax = new HighPrecisionMember(max.toString());
		}

		for (int y = 0; y < view.d1(); y++) {
			for (int x = 0; x < view.d0(); x++) {

				view.get(x, y, value);
				
				if (isHighPrec) {
					((HighPrecRepresentation) value).toHighPrec(hpPix);
				}
				else {
					throw new IllegalArgumentException("this algo kind of requires high precs");
					// expensive here
					//String pxStrValue = value.toString();
					//hpPix = new HighPrecisionMember(pxStrValue);
				}

				// scale the current value to an intensity from 0 to 1.
				
				BigDecimal numer = hpPix.v().subtract(hpMin.v());
				BigDecimal denom = hpMax.v().subtract(hpMin.v());
				
				// image with zero display range
				if (denom.compareTo(BigDecimal.ZERO) == 0)
					denom = BigDecimal.ONE;
				
				BigDecimal ratio = numer.divide(denom, HighPrecisionAlgebra.getContext());
				
				if (ratio.compareTo(BigDecimal.ZERO) < 0)
					ratio = BigDecimal.ZERO;

				if (ratio.compareTo(BigDecimal.ONE) > 0)
					ratio = BigDecimal.ONE;
				
				// now scale 0-1 to the range of the size of the current color table
				
				int intensity =
						BigDecimal.valueOf(colorTable.length-1).multiply(ratio).add(BigDecimalUtils.ONE_HALF).intValue();

				// put a color from the color table into the image at pos (x,y)
				
				int bufferPos = y * view.d0() + x;
				
				arrayInt[bufferPos] = colorTable[intensity];
			}
		}
	}
}
