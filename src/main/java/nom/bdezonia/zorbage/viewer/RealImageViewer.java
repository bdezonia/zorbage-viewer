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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import nom.bdezonia.zorbage.algebra.Addition;
import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algebra.Allocatable;
import nom.bdezonia.zorbage.algebra.Bounded;
import nom.bdezonia.zorbage.algebra.G;
import nom.bdezonia.zorbage.algebra.GetComplex;
import nom.bdezonia.zorbage.algebra.HighPrecRepresentation;
import nom.bdezonia.zorbage.algebra.Infinite;
import nom.bdezonia.zorbage.algebra.InverseTrigonometric;
import nom.bdezonia.zorbage.algebra.Invertible;
import nom.bdezonia.zorbage.algebra.Multiplication;
import nom.bdezonia.zorbage.algebra.NaN;
import nom.bdezonia.zorbage.algebra.Ordered;
import nom.bdezonia.zorbage.algebra.RealConstants;
import nom.bdezonia.zorbage.algebra.Roots;
import nom.bdezonia.zorbage.algebra.SetComplex;
import nom.bdezonia.zorbage.algebra.Trigonometric;
import nom.bdezonia.zorbage.algebra.Unity;
import nom.bdezonia.zorbage.algorithm.FFT;
import nom.bdezonia.zorbage.algorithm.GetIValues;
import nom.bdezonia.zorbage.algorithm.GetRValues;
import nom.bdezonia.zorbage.algorithm.MakeColorDatasource;
import nom.bdezonia.zorbage.algorithm.MinMaxElement;
import nom.bdezonia.zorbage.algorithm.NdSplit;
import nom.bdezonia.zorbage.algorithm.PolarCoords;
import nom.bdezonia.zorbage.coordinates.CoordinateSpace;
import nom.bdezonia.zorbage.coordinates.LinearNdCoordinateSpace;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.data.DimensionedStorage;
import nom.bdezonia.zorbage.data.NdData;
import nom.bdezonia.zorbage.datasource.FixedSizeDataSource;
import nom.bdezonia.zorbage.datasource.IndexedDataSource;
import nom.bdezonia.zorbage.datasource.ProcedurePaddedDataSource;
import nom.bdezonia.zorbage.dataview.PlaneView;
import nom.bdezonia.zorbage.dataview.TwoDView;
import nom.bdezonia.zorbage.misc.BigDecimalUtils;
import nom.bdezonia.zorbage.procedure.Procedure2;
import nom.bdezonia.zorbage.sampling.IntegerIndex;
import nom.bdezonia.zorbage.storage.Storage;
import nom.bdezonia.zorbage.type.color.ArgbAlgebra;
import nom.bdezonia.zorbage.type.color.ArgbMember;
import nom.bdezonia.zorbage.type.color.RgbUtils;
import nom.bdezonia.zorbage.type.integer.int8.UnsignedInt8Member;
import nom.bdezonia.zorbage.type.real.float128.Float128Algebra;
import nom.bdezonia.zorbage.type.real.float128.Float128Member;
import nom.bdezonia.zorbage.type.real.float16.Float16Algebra;
import nom.bdezonia.zorbage.type.real.float16.Float16Member;
import nom.bdezonia.zorbage.type.real.float32.Float32Algebra;
import nom.bdezonia.zorbage.type.real.float32.Float32Member;
import nom.bdezonia.zorbage.type.real.float64.Float64Algebra;
import nom.bdezonia.zorbage.type.real.float64.Float64Member;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionAlgebra;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionMember;
import nom.bdezonia.zorbage.type.universal.PrimitiveConversion;
import nom.bdezonia.zorbage.type.universal.PrimitiveConverter;

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
	private final Font font = new Font("Verdana", Font.PLAIN, 18);
	private AtomicBoolean animatingRightNow = new AtomicBoolean();
	private HighPrecisionMember dispMin = null;
	private HighPrecisionMember dispMax = null;
	private final JLabel ctrXLabel;
	private final JLabel ctrYLabel;
	private final DecimalFormat df = new DecimalFormat("0.000");
	private static final int MIN_MAX_CHAR_COUNT = 15;
	private static final int DISP_MIN_MAX_CHAR_COUNT = MIN_MAX_CHAR_COUNT - 5;

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
		if (dataType != null && dataType.length() != 0)
			valueInfo = dataType;
	
		// degrees K, mHg, km/h, etc
		
		String valueUnit = "(unknown unit)";
		if (dataUnit != null && dataUnit.length() != 0)
			valueUnit = " (" + dataUnit + ")";
		
		frame = new JFrame(title);
		
		frame.setLocationByPlatform(true);
		
		frame.setLayout(new BorderLayout());
		
		argbData = new BufferedImage(pz.paneWidth, pz.paneHeight, BufferedImage.TYPE_INT_ARGB);
		
		positionLabels = new JLabel[planeData.getPositionsCount()];
		for (int i = 0; i < positionLabels.length; i++) {
			positionLabels[i] = new JLabel();
			positionLabels[i].setFont(font);
		}

		JLabel dispMinLabel = new JLabel("Display Min: ");
		dispMinLabel.setFont(font);
		
		JLabel dispMaxLabel = new JLabel("Display Max: ");
		dispMaxLabel.setFont(font);

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
		JButton loadLut = new JButton("Load LUT ...");
		JButton resetLut = new JButton("Reset LUT");
		JButton swapAxes = new JButton("Swap Axes ...");
		JButton snapshot = new JButton("1X Snapshot");
		JButton incZoom = new JButton("Zoom In");
		JButton decZoom = new JButton("Zoom Out");
		JButton panLeft = new JButton("Pan Left");
		JButton panRight = new JButton("Pan Right");
		JButton panUp = new JButton("Pan Up");
		JButton panDown = new JButton("Pan Down");
		JButton resetZoom = new JButton("Reset Pan/Zoom");
		JButton dispRange = new JButton("Display Range ...");
		JButton toColor = new JButton("To Color");
		JButton toFloat = new JButton("To Float ...");
		JButton explode = new JButton("Explode ...");
		JButton fft = new JButton("FFT");
		Dimension size = new Dimension(150, 40);
		loadLut.setMinimumSize(size);
		resetLut.setMinimumSize(size);
		incZoom.setMinimumSize(size);
		decZoom.setMinimumSize(size);
		panLeft.setMinimumSize(size);
		panRight.setMinimumSize(size);
		panUp.setMinimumSize(size);
		panDown.setMinimumSize(size);
		resetZoom.setMinimumSize(size);
		dispRange.setMinimumSize(size);
		toColor.setMinimumSize(size);
		toFloat.setMinimumSize(size);
		snapshot.setMinimumSize(size);
		swapAxes.setMinimumSize(size);
		explode.setMinimumSize(size);
		fft.setMinimumSize(size);
		loadLut.setMaximumSize(size);
		resetLut.setMaximumSize(size);
		incZoom.setMaximumSize(size);
		decZoom.setMaximumSize(size);
		panLeft.setMaximumSize(size);
		panRight.setMaximumSize(size);
		panUp.setMaximumSize(size);
		panDown.setMaximumSize(size);
		resetZoom.setMaximumSize(size);
		dispRange.setMaximumSize(size);
		toColor.setMaximumSize(size);
		toFloat.setMaximumSize(size);
		snapshot.setMaximumSize(size);
		swapAxes.setMaximumSize(size);
		explode.setMaximumSize(size);
		fft.setMaximumSize(size);
		Box vertBox = Box.createVerticalBox();
		vertBox.add(loadLut);
		vertBox.add(resetLut);
		vertBox.add(incZoom);
		vertBox.add(decZoom);
		vertBox.add(panLeft);
		vertBox.add(panRight);
		vertBox.add(panUp);
		vertBox.add(panDown);
		vertBox.add(resetZoom);
		vertBox.add(dispRange);
		vertBox.add(toColor);
		vertBox.add(toFloat);
		vertBox.add(snapshot);
		vertBox.add(swapAxes);
		vertBox.add(explode);
		vertBox.add(fft);
		buttonPanel.add(vertBox);
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
						new RealImageViewer<>(alg, dataSource, i0, i1);
					//else
					//	System.out.println("" + i0 + " " + i1 + " " + iOthers);
				}
			}
		});
		snapshot.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {

				DimensionedDataSource<U> snap = pz.takeSnapshot();

				new RealImageViewer<>(alg, snap);
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
		dispRange.addActionListener(new ActionListener() {
			
			boolean cancelled = false;
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog dlg = new JDialog(frame, "", Dialog.ModalityType.DOCUMENT_MODAL);
				dlg.getContentPane().setLayout(new BoxLayout(dlg.getContentPane(), BoxLayout.Y_AXIS));
				dlg.add(new JLabel("Choose two values that specify the range of values to display"));
				JButton clrButton = new JButton("Clear");
				dlg.add(clrButton);
				dlg.add(new JLabel("Min displayable value"));
				JTextField minField = new JTextField(20);
				minField.setText(dispMin == null ? "" : dispMin.toString());
				dlg.add(minField);
				dlg.add(new JLabel("Max displayable value"));
				JTextField maxField = new JTextField(20);
				maxField.setText(dispMax == null ? "" : dispMax.toString());
				dlg.add(maxField);
				clrButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						dispMin = null;
						dispMax = null;
						minField.setText("");
						maxField.setText("");
					}
				});
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
					String minStr = minField.getText();
					String maxStr = maxField.getText();
					if (minStr == null || minStr.length() == 0) {
						dispMin = null;
					}
					else {
						dispMin = G.HP.construct(minStr);
					}
					if (maxStr == null || maxStr.length() == 0) {
						dispMax = null;
					}
					else {
						dispMax = G.HP.construct(maxStr);
					}
					String dispMinStr = (dispMin == null ? minStr : dispMin.toString());
					String dispMaxStr = (dispMin == null ? maxStr : dispMax.toString());
					dispMinLabel.setToolTipText(dispMinStr);
					dispMaxLabel.setToolTipText(dispMaxStr);
					if (dispMinStr.length() > DISP_MIN_MAX_CHAR_COUNT) dispMinStr = dispMinStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
					if (dispMaxStr.length() > DISP_MIN_MAX_CHAR_COUNT) dispMaxStr = dispMaxStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
					dispMinLabel.setText("Disp Min: " + dispMinStr);
					dispMaxLabel.setText("Disp Max: " + dispMaxStr);
					pz.draw();
					frame.repaint();
				}
			}
		});
		toColor.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				U value = alg.construct();
				DimensionedDataSource<ArgbMember> dataSource = null;
				if (value instanceof UnsignedInt8Member) {
					DimensionedDataSource<UnsignedInt8Member> casted = 
							(DimensionedDataSource<UnsignedInt8Member>) planeData.getDataSource();
					// search for channel dim: start at far end of dimensions
					int channelDim = -1;
					for (int i = casted.numDimensions()-1; i >= 0; i--) {
						long theVal = casted.dimension(i);
						if (theVal == 3 || theVal == 4) {
							channelDim = i;
							break;
						}
					}
					if (channelDim != -1)
						dataSource = MakeColorDatasource.compute(casted, channelDim);
					else
						JOptionPane.showMessageDialog(frame,
							    "Image is not 3 or 4 channel data. Cannot make color image.",
							    "WARNING",
							    JOptionPane.WARNING_MESSAGE);
				}
				else
					JOptionPane.showMessageDialog(frame,
						    "Image is not unsigned 8 bit type. Cannot make color image.",
						    "WARNING",
						    JOptionPane.WARNING_MESSAGE);
				if (dataSource != null)
					new RgbColorImageViewer<ArgbAlgebra,ArgbMember>(G.ARGB, dataSource);
			}
		});
		toFloat.addActionListener(new ActionListener() {

			boolean cancelled = false;
			Algebra<?,?> fltAlg = null;
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog dlg = new JDialog(frame, "", Dialog.ModalityType.DOCUMENT_MODAL);
				dlg.setLayout(new FlowLayout());
				dlg.add(new JLabel("Choose floating point type of output"));
				ButtonGroup bg = new ButtonGroup();
				JRadioButton f16 = new JRadioButton("16 bit float");
				JRadioButton f32 = new JRadioButton("32 bit float");
				JRadioButton f64 = new JRadioButton("64 bit float");
				JRadioButton f128 = new JRadioButton("128 bit float");
				JRadioButton fhp = new JRadioButton("Unbounded float");
				bg.add(f16);
				bg.add(f32);
				bg.add(f64);
				bg.add(f128);
				bg.add(fhp);
				dlg.add(f16);
				dlg.add(f32);
				dlg.add(f64);
				dlg.add(f128);
				dlg.add(fhp);
				f16.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						fltAlg = G.HLF;
					}
				});
				f32.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						fltAlg = G.FLT;
					}
				});
				f64.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						fltAlg = G.DBL;
					}
				});
				f128.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						fltAlg = G.QUAD;
					}
				});
				fhp.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						fltAlg = G.HP;
					}
				});
				
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
				if (!cancelled && fltAlg != null) {
					convertToFloat(fltAlg, alg, planeData.getDataSource());
				}
			}
		});
		explode.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				DimensionedDataSource<U> dataSource = planeData.getDataSource();
				String input = JOptionPane.showInputDialog("Choose axis number along which the data will be exploded (0 - "+(dataSource.numDimensions()-1)+")");
				try {
					int axis = Integer.parseInt(input);
					if (axis >= 0 || axis < dataSource.numDimensions()) {
						explode(dataSource, axis);
					}
				} catch (NumberFormatException exc) {
					;
				}
			}
		});
		fft.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				DimensionedDataSource<U> data = planeData.getDataSource();
				
				if (min instanceof Float16Member) {
					doFFT(G.CHLF, G.HLF, data);
				}
				else if (min instanceof Float32Member) {
					doFFT(G.CFLT, G.FLT, data);
				}
				else if (min instanceof Float64Member) {
					doFFT(G.CDBL, G.DBL, data);
				}
				else if (min instanceof Float128Member) {
					doFFT(G.CQUAD, G.QUAD, data);
				}
				else if (min instanceof HighPrecisionMember) {
					doFFT(G.CHP, G.HP, data);
				}
				else
					JOptionPane.showMessageDialog(frame,
						    "Image is not in a recognizable floating point format.",
						    "WARNING",
						    JOptionPane.WARNING_MESSAGE);
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

			HighPrecisionMember hpVal = G.HP.construct();
			long[] modelCoords = new long[dataSource.numDimensions()];
			BigDecimal[] realWorldCoords = new BigDecimal[dataSource.numDimensions()]; 
			U value = alg.construct();
					
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
						sb.append(df.format(hpVal.v()));
					sb.append(" ");
					sb.append(dataSource.getValueUnit() == null ? "" : dataSource.getValueUnit());
					readout.setText(sb.toString());
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
			}
		});
		
		JCheckBox check = new JCheckBox("Use data range");
		check.setSelected(preferDataRange);
		check.setFont(font);

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
		miscPanel.add(check);
		miscPanel.add(new JSeparator());
		JLabel minLabel = new JLabel("Min: ");
		minLabel.setFont(font);
		JLabel maxLabel = new JLabel("Max: ");
		maxLabel.setFont(font);
		miscPanel.add(minLabel);
		miscPanel.add(maxLabel);
		miscPanel.add(new JSeparator());
		miscPanel.add(dispMinLabel);
		miscPanel.add(dispMaxLabel);
		miscPanel.add(new JSeparator());
		check.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				preferDataRange = !preferDataRange;
				calcMinMax();
				String minStr = min.toString();
				String maxStr = max.toString();
				String dispMinStr = (dispMin == null ? minStr : dispMin.toString());
				String dispMaxStr = (dispMin == null ? maxStr : dispMax.toString());
				minLabel.setToolTipText(minStr);
				maxLabel.setToolTipText(maxStr);
				dispMinLabel.setToolTipText(dispMinStr);
				dispMaxLabel.setToolTipText(dispMaxStr);
				if (minStr.length() > MIN_MAX_CHAR_COUNT) minStr = minStr.substring(0,MIN_MAX_CHAR_COUNT) + "...";
				if (maxStr.length() > MIN_MAX_CHAR_COUNT) maxStr = maxStr.substring(0,MIN_MAX_CHAR_COUNT) + "...";
				if (dispMinStr.length() > DISP_MIN_MAX_CHAR_COUNT) dispMinStr = dispMinStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
				if (dispMaxStr.length() > DISP_MIN_MAX_CHAR_COUNT) dispMaxStr = dispMaxStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
				minLabel.setText("Min: " + minStr);
				maxLabel.setText("Max: " + maxStr);
				dispMinLabel.setText("Disp Min: " + dispMinStr);
				dispMaxLabel.setText("Disp Max: " + dispMaxStr);
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
		
		String minStr = min.toString();
		String maxStr = max.toString();
		String dispMinStr = (dispMin == null ? minStr : dispMin.toString());
		String dispMaxStr = (dispMin == null ? maxStr : dispMax.toString());
		minLabel.setToolTipText(minStr);
		maxLabel.setToolTipText(maxStr);
		dispMinLabel.setToolTipText(dispMinStr);
		dispMaxLabel.setToolTipText(dispMaxStr);
		if (minStr.length() > MIN_MAX_CHAR_COUNT) minStr = minStr.substring(0,MIN_MAX_CHAR_COUNT) + "...";
		if (maxStr.length() > MIN_MAX_CHAR_COUNT) maxStr = maxStr.substring(0,MIN_MAX_CHAR_COUNT) + "...";
		if (dispMinStr.length() > DISP_MIN_MAX_CHAR_COUNT) dispMinStr = dispMinStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
		if (dispMaxStr.length() > DISP_MIN_MAX_CHAR_COUNT) dispMaxStr = dispMaxStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
		minLabel.setText("Min: " + minStr);
		maxLabel.setText("Max: " + maxStr);
		dispMinLabel.setText("Disp Min: " + dispMinStr);
		dispMaxLabel.setText("Disp Max: " + dispMaxStr);

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

		// TODO: use BigInts in calcs?		

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

		// TODO: use BigInts in calcs?
		
		public long getVirtualOriginX() {
			return originX;
		}
		
		// TODO: use BigInts in calcs?
		
		public long getVirtualOriginY() {
			return originY;
		}
		
		// TODO: use BigInts in calcs?
		
		public long getVirtualWidth() {
			return calculatedPaneWidth;
		}
		
		// TODO: use BigInts in calcs?
		
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
		
		// TODO: use BigInts in calcs?
		
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
		
		
		// TODO: use BigInts in calcs?
		
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

		// TODO: use BigInts in calcs?
		
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

		// TODO: use BigInts in calcs?
		
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

		// TODO: use BigInts in calcs?
		
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

		// TODO: use BigInts in calcs?
		
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

			// Reduce flicker while not needing to make any changes for double buffering or page flipping.
			
			Toolkit.getDefaultToolkit().sync();
			
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
			long maxDimX = planeData.d0();
			long maxDimY = planeData.d1();
			for (int y = 0; y < paneHeight; y++) {
				for (int x = 0; x < paneWidth; x++) {
					G.HP.zero().call(sum);
					boolean includesNans = false; 
					boolean includesPosInfs = false; 
					boolean includesNegInfs = false; 
					long numCounted = 0;
					boolean modelCoordsInBounds = false;
					long mx = pixelToModel(x, originX);
					long my = pixelToModel(y, originY);
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
					int color = 0;
					if (modelCoordsInBounds) {

						// calc average intensity

						BigDecimal avgIntensity = getIntensity(sum, numCounted, includesNans, includesPosInfs, includesNegInfs);
						
						color = getColor(avgIntensity);
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
			if (dispMin != null && G.HP.isGreater().call(dispMin, hpMin))
				G.HP.assign().call(dispMin, hpMin);
			if (dispMax != null && G.HP.isLess().call(dispMax, hpMax))
				G.HP.assign().call(dispMax, hpMax);

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
		 * as much as is feasible. Snapshot is taken at 1x at origin
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
			String miniTitle = axes + " slice";
			String extendedDims = "";
			if (origDs.numDimensions() > 2) {
				extendedDims = " at";
				int count = 0;;
				for (int i = 0; i < origDs.numDimensions(); i++) {
					if (i == axisNumber0 || i == axisNumber1)
						continue;
					String axisLabel = origDs.getAxisType(i);
					long pos = planeData.getPositionValue(count);
					count++;
					extendedDims = extendedDims + " " + axisLabel + "("+pos+")"; 
				}
			}
			miniTitle = miniTitle + extendedDims;

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
	
	public <L extends Algebra<L,M> & Addition<M> & Multiplication<M>,
			M extends SetComplex<O> & GetComplex<O> & Allocatable<M>,
			N extends Algebra<N,O> & Trigonometric<O> & RealConstants<O> &
				Multiplication<O> & Addition<O> & Invertible<O> & Unity<O> &
				NaN<O> & InverseTrigonometric<O> & Roots<O> & Ordered<O>,
			O extends Allocatable<O>>
		void doFFT(Algebra<?,?> complexAlgebra, Algebra<?,?> realAlgebra, DimensionedDataSource<?> input)
	{
		String error = null;
		
		if (!(complexAlgebra instanceof Addition))
			error = "Complex algebra does not implement Addition";
		
		else if (!(complexAlgebra instanceof Multiplication))
			error = "Complex algebra does not implement Multiplication";
		
		@SuppressWarnings("unchecked")
		L cmplxAlg = (L) complexAlgebra;
		
		M tmpM = cmplxAlg.construct();
		
		if (!(tmpM instanceof SetComplex))
			error = "Complex number does not implement SetComplex";
		
		else if (!(tmpM instanceof GetComplex))
			error = "Complex number does not implement GetComplex";
		
		else if (!(tmpM instanceof Allocatable))
			error = "Complex number does not implement Allocatable";
		
		else if (!(realAlgebra instanceof Trigonometric))
			error = "Real algebra does not implement Trigonometric";
		
		else if (!(realAlgebra instanceof RealConstants))
			error = "Real algebra does not implement RealConstants";
		
		else if (!(realAlgebra instanceof Multiplication))
			error = "Real algebra does not implement Multiplication";
		
		else if (!(realAlgebra instanceof Addition))
			error = "Real algebra does not implement Addition";
		
		else if (!(realAlgebra instanceof Invertible))
			error = "Real algebra does not implement Invertible";
		
		else if (!(realAlgebra instanceof Unity))
			error = "Real algebra does not implement Unity";

		else if (!(realAlgebra instanceof NaN))
			error = "Real algebra does not implement NaN";
		
		else if (!(realAlgebra instanceof InverseTrigonometric))
			error = "Real algebra does not implement InverseTrigonometric";
		
		else if (!(realAlgebra instanceof Roots))
			error = "Real algebra does not implement Roots";
		
		else if (!(realAlgebra instanceof Ordered))
			error = "Real algebra does not implement Ordered";
		
		@SuppressWarnings("unchecked")
		N realAlg = (N) realAlgebra;
		
		O tmpO = realAlg.construct();
		
		try {
			
			tmpM.setR(tmpO);
			tmpM.setI(tmpO);
			
		} catch (ClassCastException e) {
			
			error = "Complex algebra and real algebra are not compatible";
		}
		
		try {
			
			tmpM.getR(tmpO);
			tmpM.getI(tmpO);
			
		} catch (ClassCastException e) {
			
			error = "Complex algebra and real algebra are not compatible";
		}
		
		if (error != null) {
			JOptionPane.showMessageDialog(frame,
				    "FFT error: ."+error,
				    "WARNING",
				    JOptionPane.WARNING_MESSAGE);
			return;
		}

		long successfulSize = -1;
		long edgeSize = -1;
		for (long i = 0; i < 63; i++) {
			
			edgeSize = 1 << i;
			
			long sqSz = edgeSize * edgeSize;
			
			if (sqSz >= input.rawData().size()) {
				successfulSize = sqSz;
				break;
			}
		}

		if ((successfulSize < 0)) {
			throw new IllegalArgumentException("can't find an enclosing image size for FFT");
		}
		
		// zero pad the real input
		@SuppressWarnings("unchecked")
		IndexedDataSource<O> padded =
				new ProcedurePaddedDataSource<N,O>(
						realAlg,
						(IndexedDataSource<O>) input.rawData(),
						new Procedure2<Long, O>()
						{
							public void call(Long a, O b) {
								realAlg.zero().call(b);
							}
						}
					);
		
		// and make sure its size is a power of 2
		
		IndexedDataSource<O> correctSizeInput = new FixedSizeDataSource<>(successfulSize, padded);
		
		// now make a complex data source with real values set to input
		
		IndexedDataSource<M> complexInput = Storage.allocate(cmplxAlg.construct(), successfulSize);
		
		// And define where we will put the result
		
		IndexedDataSource<M> complexOutput = Storage.allocate(cmplxAlg.construct(), successfulSize);

		// copy the real data to the r values of a complex data set
		
		O realValue = realAlg.construct();
		O imagValue = realAlg.construct();
		O zero = realAlg.construct();
		M complexValue = cmplxAlg.construct();
		for (long i = 0; i < successfulSize; i++) {
			correctSizeInput.get(i, realValue);
			complexValue.setR(realValue);
			complexValue.setI(zero);
			complexInput.set(i, complexValue);
		}
		
		// run the FFT on that data
		
		FFT.compute(cmplxAlg, realAlg, complexInput, complexOutput);
		
		long sz = complexOutput.size();
		
		IndexedDataSource<O> m = Storage.allocate(realValue, sz);
		IndexedDataSource<O> p = Storage.allocate(realValue, sz);
		
		GetRValues.compute(cmplxAlg, realAlg, complexOutput, m);
		GetIValues.compute(cmplxAlg, realAlg, complexOutput, p);
		
		O mag = realAlg.construct();
		O phas = realAlg.construct();
		for (long i = 0; i < sz; i++) {
			m.get(i, realValue);
			p.get(i, imagValue);
			PolarCoords.magnitude(realAlg, realValue, imagValue, mag);
			PolarCoords.phase(realAlg, realValue, imagValue, phas);
			m.set(i, mag);
			p.set(i, phas);
		}

		DimensionedDataSource<O> magDs =
				new NdData<O>(new long[] {edgeSize, edgeSize}, m);

		DimensionedDataSource<O> phasDs =
				new NdData<O>(new long[] {edgeSize, edgeSize}, p);

		magDs.setName("Magnitudes of FFT of "+input.getName());
		
		phasDs.setName("Phases of FFT of "+input.getName());

		new RealImageViewer<>(realAlg, magDs);

		new RealImageViewer<>(realAlg, phasDs);
		
		/*
		DimensionedDataSource<M> complexDs =
				new NdData<M>(new long[] {edgeSize, edgeSize}, complexOutput);

		complexDs.setName("FFT of "+input.getName());
		
		complexDs.setSource(input.getSource());
		
		new ComplexImageViewer<L,M,N,O>(cmplxAlg, realAlg, complexDs);
		*/
	}
	
	<L extends Algebra<L,M>, M extends PrimitiveConversion & Allocatable<M>, N extends Algebra<N,O>, O>
		void convertToFloat(Algebra<?,?> oAlg, N inAlg, DimensionedDataSource<O> input)
	{
		if (!(oAlg instanceof Float16Algebra) && !(oAlg instanceof Float32Algebra) &&
				!(oAlg instanceof Float64Algebra) && !(oAlg instanceof Float128Algebra) &&
				!(oAlg instanceof HighPrecisionAlgebra))
		{
			JOptionPane.showMessageDialog(frame,
				    "To float command not passed a real algebra to use for the output type",
				    "WARNING",
				    JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		if (!(inAlg.construct() instanceof PrimitiveConversion)) {
			JOptionPane.showMessageDialog(frame,
				    "To float command input type does not support primitive conversion",
				    "WARNING",
				    JOptionPane.WARNING_MESSAGE);
		}

		int numD = input.numDimensions();
		
		long[] dims = new long[numD];
		for (int i = 0; i < numD; i++) {
			dims[i] = input.dimension(i);
		}
		
		IntegerIndex tmp1 = new IntegerIndex(0);
		IntegerIndex tmp2 = new IntegerIndex(0);
		IntegerIndex tmp3 = new IntegerIndex(0);
		
		@SuppressWarnings("unchecked")
		L outAlg = (L) oAlg;
		
		DimensionedDataSource<M> output = DimensionedStorage.allocate(outAlg.construct(), dims);

		IndexedDataSource<O> inList =  input.rawData();  
		IndexedDataSource<M> outList =  output.rawData();

		O in =  inAlg.construct();
		M out = outAlg.construct();

		long size = inList.size();
		for (long i = 0; i < size; i++) {
			inList.get(i, in);
			PrimitiveConverter.convert(tmp1, tmp2, tmp3, (PrimitiveConversion) in, out);
			outList.set(i, out);
		}
		
		// TODO: copy metadata to output!
		
		new RealImageViewer<L,M>(outAlg, output);
	}
	
	@SuppressWarnings("unchecked")
	<V extends Algebra<V,W>, W extends Allocatable<W>>
		void explode(DimensionedDataSource<U> dataSource, int axis)
	{
		V enhancedAlg = (V) alg;
		DimensionedDataSource<W> ds = (DimensionedDataSource<W>) dataSource;
		List<DimensionedDataSource<W>>  results = NdSplit.compute(enhancedAlg, axis, 1L, ds);
		for (DimensionedDataSource<W> dataset : results) {
			new RealImageViewer<V,W>(enhancedAlg, dataset);
		}
	}
}
