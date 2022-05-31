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
 *   budget
  burgers

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
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.URL;
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
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import nom.bdezonia.zorbage.algebra.Addition;
import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algebra.Allocatable;
import nom.bdezonia.zorbage.algebra.Bounded;
import nom.bdezonia.zorbage.algebra.Conjugate;
import nom.bdezonia.zorbage.algebra.Exponential;
import nom.bdezonia.zorbage.algebra.G;
import nom.bdezonia.zorbage.algebra.GetComplex;
import nom.bdezonia.zorbage.algebra.HighPrecRepresentation;
import nom.bdezonia.zorbage.algebra.Hyperbolic;
import nom.bdezonia.zorbage.algebra.Infinite;
import nom.bdezonia.zorbage.algebra.InverseTrigonometric;
import nom.bdezonia.zorbage.algebra.Invertible;
import nom.bdezonia.zorbage.algebra.Multiplication;
import nom.bdezonia.zorbage.algebra.NaN;
import nom.bdezonia.zorbage.algebra.Ordered;
import nom.bdezonia.zorbage.algebra.Power;
import nom.bdezonia.zorbage.algebra.RealConstants;
import nom.bdezonia.zorbage.algebra.Roots;
import nom.bdezonia.zorbage.algebra.SetComplex;
import nom.bdezonia.zorbage.algebra.Trigonometric;
import nom.bdezonia.zorbage.algebra.Unity;
import nom.bdezonia.zorbage.algorithm.FFT2D;
import nom.bdezonia.zorbage.algorithm.GetIValues;
import nom.bdezonia.zorbage.algorithm.GetRValues;
import nom.bdezonia.zorbage.algorithm.InvFFT2D;
import nom.bdezonia.zorbage.algorithm.MakeColorDatasource;
import nom.bdezonia.zorbage.algorithm.MinMaxElement;
import nom.bdezonia.zorbage.algorithm.NdSplit;
import nom.bdezonia.zorbage.algorithm.PolarCoords;
import nom.bdezonia.zorbage.algorithm.Transform2;
import nom.bdezonia.zorbage.coordinates.CoordinateSpace;
import nom.bdezonia.zorbage.coordinates.LinearNdCoordinateSpace;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.data.DimensionedStorage;
import nom.bdezonia.zorbage.data.NdData;
import nom.bdezonia.zorbage.datasource.IndexedDataSource;
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

	private static final URL ICON_URL = Main.class.getClassLoader().getResource("construction.gif");
	
	private static int desiredWidth = 512;
	private static int desiredHeight = 512;
	
	private final T alg;
	private final PlaneView<U> planeData;
	private final PanZoomView pz;
	private final BufferedImage argbData;
	private int[] colorTable = LutUtils.DEFAULT_COLOR_TABLE;
	private boolean preferDataRange = true;
	private final U min;
	private final U max;
	private final U dataMin;
	private final U dataMax;
	private final U typeMin;
	private final U typeMax;
	private final JLabel[] positionLabels;
	private final JFrame frame;
	private NaN<U> nanTester = null;
	private Infinite<U> infTester = null;
	private Ordered<U> signumTester = null;
	private final Font font = new Font("Verdana", Font.PLAIN, 18);
	private AtomicBoolean animatingRightNow = new AtomicBoolean();
	private AtomicBoolean pleaseQuitAnimating = new AtomicBoolean();
	private HighPrecisionMember dispMin = null;
	private HighPrecisionMember dispMax = null;
	private final JLabel ctrXLabel;
	private final JLabel ctrYLabel;
	private final JLabel constructionLabel;
	private final DecimalFormat df = new DecimalFormat("0.00000");
	private static final int MIN_MAX_CHAR_COUNT = 15;
	private static final int DISP_MIN_MAX_CHAR_COUNT = MIN_MAX_CHAR_COUNT - 5;

	/**
	 * Make an interactive graphical viewer for a real data source.
	 * @param alg The algebra that matches the type of data to display
	 * @param dataSource The data to display
	 */
	public RealImageViewer(T alg, DimensionedDataSource<U> dataSource) {
		this(alg, dataSource, 0, 1, null, null);
	}

	/**
	 * Make an interactive graphical viewer for a real data source.
	 * @param alg The algebra that matches the type of data to display
	 * @param dataSource The data to display
	 * @param axisNumber0 The first axis number defining the planes to view (x, y, z, c, t, etc.)
	 * @param axisNumber1 The second axis number defining the planes to view (x, y, z, c, t, etc.)
	 * @param dataMn A (possibly null) hint at the actual min data value present in data source
	 * @param dataMx A (possibly null) hint at the actual max data value present in data source
	 */
	@SuppressWarnings("unchecked")
	public RealImageViewer(T alg, DimensionedDataSource<U> dataSource, int axisNumber0, int axisNumber1, U dataMn, U dataMx) {

		this.alg = alg;
		this.planeData = new PlaneView<>(dataSource, axisNumber0, axisNumber1);
		this.pz = new PanZoomView(desiredWidth, desiredHeight);
		this.min = alg.construct();
		this.max = alg.construct();
		this.dataMin = alg.construct();
		this.dataMax = alg.construct();
		this.typeMin = alg.construct();
		this.typeMax = alg.construct();

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
		
		String source = dataSource.getSource();
		
		String title = "Zorbage Viewer - "+name;
	
		// temperature, pressure, speed, etc
		
		String valueType = dataSource.getValueType();

		// degrees K, mHg, km/h, etc
		
		String valueUnit = dataSource.getValueUnit();

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

		JPanel headerPanelLeft = new JPanel();
		Box box = Box.createVerticalBox();
		box.add(sourceLabel);
		JLabel a = new JLabel("Data type: " + alg.typeDescription());
		JLabel b = new JLabel("Data family: " + valueType);
		JLabel c = new JLabel("Data unit: " + valueUnit);
		a.setFont(font);
		b.setFont(font);
		c.setFont(font);
		box.add(a);
		box.add(b);
		box.add(c);
		headerPanelLeft.add(box);
		
		JPanel headerPanelRight = new JPanel();
		// place holder for "construction" image ...
		constructionLabel = new JLabel();
		constructionLabel.setIcon(new ImageIcon());
		headerPanelRight.add(constructionLabel);
		
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BorderLayout());
		headerPanel.add(headerPanelLeft, BorderLayout.WEST);
		headerPanel.add(headerPanelRight, BorderLayout.EAST);

		JPanel graphicsPanel = new JPanel();
		JLabel image = new JLabel(new ImageIcon(argbData));
		JScrollPane scrollPane = new JScrollPane(image);
		graphicsPanel.add(scrollPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton loadLut = new JButton("Load LUT ...");
		JButton resetLut = new JButton("Reset LUT");
		JButton swapAxes = new JButton("Swap Axes ...");
		JButton snapshot = new JButton("Snapshot");
		JButton grabPlane = new JButton("Grab Plane");
		JButton incZoom = new JButton("Zoom In");
		JButton decZoom = new JButton("Zoom Out");
		JButton panLeft = new JButton("Pan Left");
		JButton panRight = new JButton("Pan Right");
		JButton panUp = new JButton("Pan Up");
		JButton panDown = new JButton("Pan Down");
		JButton resetZoom = new JButton("Reset Pan/Zoom");
		JButton dispRange = new JButton("Display Range ...");
		JButton winSize = new JButton("Window Size ...");
		JButton toColor = new JButton("To Color");
		JButton toFloat = new JButton("To Float ...");
		JButton explode = new JButton("Explode ...");
		JButton fft = new JButton("FFT");
		JButton transform = new JButton("Transform");
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
		winSize.setMinimumSize(size);
		toColor.setMinimumSize(size);
		toFloat.setMinimumSize(size);
		snapshot.setMinimumSize(size);
		grabPlane.setMinimumSize(size);
		swapAxes.setMinimumSize(size);
		explode.setMinimumSize(size);
		fft.setMinimumSize(size);
		transform.setMinimumSize(size);
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
		winSize.setMaximumSize(size);
		toColor.setMaximumSize(size);
		toFloat.setMaximumSize(size);
		snapshot.setMaximumSize(size);
		grabPlane.setMaximumSize(size);
		swapAxes.setMaximumSize(size);
		explode.setMaximumSize(size);
		fft.setMaximumSize(size);
		transform.setMaximumSize(size);
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
		vertBox.add(winSize);
		vertBox.add(toColor);
		vertBox.add(toFloat);
		vertBox.add(snapshot);
		vertBox.add(grabPlane);
		vertBox.add(swapAxes);
		vertBox.add(explode);
		vertBox.add(fft);
		vertBox.add(transform);
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
				dlg.setLocationByPlatform(true);
				dlg.setLayout(new FlowLayout());
				dlg.add(new JLabel("Choose two dimensions that specify the planes of interest"));
				for (int i = 0; i < dataSource.numDimensions(); i++) {
					checked[i] = false;
					String label = "" + i + ": " + dataSource.getAxisType(i);
					JCheckBox bx = new JCheckBox(label);
					bx.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							String label = bx.getText();
							int pos = label.indexOf(':');
							int dimNum = Integer.parseInt(label.substring(0,pos));
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
						new RealImageViewer<>(alg, dataSource, i0, i1, dataMin, dataMax);
					//else
					//	System.out.println("" + i0 + " " + i1 + " " + iOthers);
				}
			}
		});
		snapshot.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {

				DimensionedDataSource<ArgbMember> snap = pz.takeSnapshot();

				new RgbColorImageViewer<>(G.ARGB, snap);
			}
		});
		grabPlane.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {

				DimensionedDataSource<U> plane = grabCurrentPlane();

				new RealImageViewer<>(alg, plane);
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
			
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Object,Object> worker = new SwingWorker<Object, Object>() {
					
					boolean cancelled = false;
					String origMinStrVal = effectiveMinToStr(); 
					String origMaxStrVal = effectiveMaxToStr(); 
					MathContext context = new MathContext(7);
					

					@Override
					protected Object doInBackground() throws Exception {
						JDialog dlg = new JDialog(frame, "", Dialog.ModalityType.DOCUMENT_MODAL);
						dlg.getContentPane().setLayout(new BoxLayout(dlg.getContentPane(), BoxLayout.Y_AXIS));
						dlg.add(new JLabel("Choose two values that specify the range of values to display"));
						JButton resetButton = new JButton("Reset");
						dlg.add(resetButton);
						dlg.add(new JLabel("Min displayable value"));
						JTextField minField = new JTextField(20);
						minField.setText(effectiveMinToStr());
						dlg.add(minField);
						BigDecimal dataRange = actualMax().subtract(actualMin());
						JScrollBar minScroll = new JScrollBar(JScrollBar.HORIZONTAL);
						minScroll.setMinimum(0);
						minScroll.setMaximum(999);
						minScroll.setUnitIncrement(1);
						minScroll.getModel().setExtent(10);
						BigDecimal effMin = effectiveMin();
						BigDecimal fraction = effMin.subtract(actualMin());
						int sliderPos = BigDecimal.valueOf(minScroll.getMaximum()).multiply(fraction).divide(dataRange, context).intValue();
						minScroll.setValue(sliderPos);
						minScroll.addAdjustmentListener(
						
							new AdjustmentListener() {

								@Override
								public void adjustmentValueChanged(AdjustmentEvent e) {
									HighPrecisionMember minHP = G.HP.construct();
									HighPrecisionMember maxHP = G.HP.construct();
									((HighPrecRepresentation) min).toHighPrec(minHP);
									((HighPrecRepresentation) max).toHighPrec(maxHP);
									int sliderMin = minScroll.getMinimum();
									int sliderMax = minScroll.getMaximum();
									int sliderValue = minScroll.getValue();
									if (500 <= sliderValue && sliderValue < 510) {
										switch (sliderValue) {
										case 500:
											sliderValue = 500;
											break;
										case 501:
											sliderValue = 503;
											break;
										case 502:
											sliderValue = 505;
											break;
										case 503:
											sliderValue = 507;
											break;
										case 504:
											sliderValue = 509;
											break;
										case 505:
											sliderValue = 511;
											break;
										case 506:
											sliderValue = 513;
											break;
										case 507:
											sliderValue = 515;
											break;
										case 508:
											sliderValue = 517;
											break;
										case 509:
											sliderValue = 519;
											break;
										}
									}
									else if (sliderValue >= 510) {
										sliderValue += minScroll.getModel().getExtent();
									}
									BigDecimal percent = BigDecimal.valueOf(sliderValue - sliderMin).divide(BigDecimal.valueOf(sliderMax - sliderMin), context);
									BigDecimal dataRange = (maxHP.v().subtract(minHP.v()));
									BigDecimal subrange = percent.multiply(dataRange, context);
									BigDecimal newValue = minHP.v().add(subrange);
									minHP.setV(newValue);
									minField.setText(newValue.toString());
									if (dispMin == null)
										dispMin = G.HP.construct();
									dispMin.fromHighPrec(minHP);
//									String dispMaxStr = effectiveMaxToStr();
									String dispMinStr = effectiveMinToStr();
									if (dispMinStr.length() > DISP_MIN_MAX_CHAR_COUNT)
										dispMinStr = dispMinStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
//									if (dispMaxStr.length() > DISP_MIN_MAX_CHAR_COUNT)
//										dispMaxStr = dispMaxStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
									dispMinLabel.setText("Disp Min: " + dispMinStr);
//									dispMaxLabel.setText("Disp Max: " + dispMaxStr);
									pz.draw();
									frame.repaint();
								}
							}
						);
						dlg.add(minScroll);
						minField.addFocusListener(new FocusListener() {
							
							@Override
							public void focusLost(FocusEvent arg0) {
								String numStr = minField.getText();
								if (numStr != null && numStr.length() > 0) {
									BigDecimal num;
									try { 
										num = new BigDecimal(numStr);
									} catch (NumberFormatException exc) {
										return;
									}
									BigDecimal numer = num.subtract(actualMin());
									BigDecimal denom = actualMax().subtract(actualMin());
									if (denom.compareTo(BigDecimal.ZERO) == 0)
										denom = BigDecimal.ONE;
									BigDecimal percent = numer.divide(denom, context);
									if (percent.compareTo(BigDecimal.ZERO) < 0)
										percent = BigDecimal.ZERO;
									if (percent.compareTo(BigDecimal.ONE) > 0)
										percent = BigDecimal.ONE;
									int pos = percent.multiply(BigDecimal.valueOf(minScroll.getMaximum())).intValue();
									minScroll.setValue(pos);
									if (dispMin == null)
										dispMin = G.HP.construct();
									dispMin.setV(percent.multiply(denom));
									String dispMinStr = effectiveMinToStr();
									if (dispMinStr.length() > DISP_MIN_MAX_CHAR_COUNT)
										dispMinStr = dispMinStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
									dispMaxLabel.setText("Disp Min: " + dispMinStr);
									pz.draw();
									frame.repaint();
								}
							}
							
							@Override
							public void focusGained(FocusEvent arg0) {
							}
						});
						dlg.add(new JLabel("Max displayable value"));
						JTextField maxField = new JTextField(20);
						maxField.setText(effectiveMaxToStr());
						dlg.add(maxField);
						JScrollBar maxScroll = new JScrollBar(JScrollBar.HORIZONTAL);
						maxScroll.setMinimum(0);
						maxScroll.setMaximum(999);
						maxScroll.setUnitIncrement(1);
						maxScroll.getModel().setExtent(10);
						BigDecimal effMax = effectiveMax();
						fraction = effMax.subtract(actualMin());
						sliderPos = BigDecimal.valueOf(maxScroll.getMaximum()).multiply(fraction).divide(dataRange, context).intValue();
						maxScroll.setValue(sliderPos);
						maxScroll.addAdjustmentListener(
							
							new AdjustmentListener() {
								
								@Override
								public void adjustmentValueChanged(AdjustmentEvent e) {
									HighPrecisionMember minHP = G.HP.construct();
									HighPrecisionMember maxHP = G.HP.construct();
									((HighPrecRepresentation) min).toHighPrec(minHP);
									((HighPrecRepresentation) max).toHighPrec(maxHP);
									int sliderMin = maxScroll.getMinimum();
									int sliderMax = maxScroll.getMaximum();
									int sliderValue = maxScroll.getValue();
									if (500 <= sliderValue && sliderValue < 510) {
										switch (sliderValue) {
										case 500:
											sliderValue = 500;
											break;
										case 501:
											sliderValue = 503;
											break;
										case 502:
											sliderValue = 505;
											break;
										case 503:
											sliderValue = 507;
											break;
										case 504:
											sliderValue = 509;
											break;
										case 505:
											sliderValue = 511;
											break;
										case 506:
											sliderValue = 513;
											break;
										case 507:
											sliderValue = 515;
											break;
										case 508:
											sliderValue = 517;
											break;
										case 509:
											sliderValue = 519;
											break;
										}
									}
									else if (sliderValue >= 510) {
										sliderValue += maxScroll.getModel().getExtent();
									}
									BigDecimal percent = BigDecimal.valueOf(sliderValue - sliderMin).divide(BigDecimal.valueOf(sliderMax - sliderMin), context);
									BigDecimal dataRange = (maxHP.v().subtract(minHP.v()));
									BigDecimal subrange = percent.multiply(dataRange, context);
									BigDecimal newValue = minHP.v().add(subrange);
									maxHP.setV(newValue);
									maxField.setText(newValue.toString());
									if (dispMax == null)
										dispMax = G.HP.construct();
									dispMax.fromHighPrec(maxHP);
//									String dispMinStr = effectiveMinToStr();
									String dispMaxStr = effectiveMaxToStr();
//									if (dispMinStr.length() > DISP_MIN_MAX_CHAR_COUNT)
//										dispMinStr = dispMinStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
									if (dispMaxStr.length() > DISP_MIN_MAX_CHAR_COUNT)
										dispMaxStr = dispMaxStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
//									dispMinLabel.setText("Disp Min: " + dispMinStr);
									dispMaxLabel.setText("Disp Max: " + dispMaxStr);
									pz.draw();
									frame.repaint();
								}
							}
						);
						dlg.add(maxScroll);
						maxField.addFocusListener(new FocusListener() {
							
							@Override
							public void focusLost(FocusEvent arg0) {
								String numStr = maxField.getText();
								if (numStr != null && numStr.length() > 0) {
									BigDecimal num;
									try { 
										num = new BigDecimal(numStr);
									} catch (NumberFormatException exc) {
										return;
									}
									BigDecimal numer = num.subtract(actualMin());
									BigDecimal denom = actualMax().subtract(actualMin());
									if (denom.compareTo(BigDecimal.ZERO) == 0)
										denom = BigDecimal.ONE;
									BigDecimal percent = numer.divide(denom, context);
									if (percent.compareTo(BigDecimal.ZERO) < 0)
										percent = BigDecimal.ZERO;
									if (percent.compareTo(BigDecimal.ONE) > 0)
										percent = BigDecimal.ONE;
									int pos = percent.multiply(BigDecimal.valueOf(maxScroll.getMaximum())).intValue();
									maxScroll.setValue(pos);
									if (dispMax == null)
										dispMax = G.HP.construct();
									dispMax.setV(percent.multiply(denom));
									String dispMaxStr = effectiveMaxToStr();
									if (dispMaxStr.length() > DISP_MIN_MAX_CHAR_COUNT)
										dispMaxStr = dispMaxStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
									dispMaxLabel.setText("Disp Max: " + dispMaxStr);
									pz.draw();
									frame.repaint();
								}
							}
							
							@Override
							public void focusGained(FocusEvent arg0) {
							}
						});
						resetButton.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								dispMin = null;
								dispMax = null;
								minField.setText(min.toString());
								maxField.setText(max.toString());
								minScroll.setValue(minScroll.getMinimum());
								maxScroll.setValue(maxScroll.getMaximum());
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
						String minStr = minField.getText();
						String maxStr = maxField.getText();
						if (cancelled) {
							minStr = origMinStrVal;
							maxStr = origMaxStrVal;
						}
						else {
							minStr = minField.getText();
							maxStr = maxField.getText();
						}
						if (minStr == null || minStr.length() == 0 || minStr.equals(min.toString())) {
							dispMin = null;
						}
						else {
							dispMin = G.HP.construct(minStr);
						}
						if (maxStr == null || maxStr.length() == 0 || maxStr.equals(max.toString())) {
							dispMax = null;
						}
						else {
							dispMax = G.HP.construct(maxStr);
						}
						String dispMinStr = effectiveMinToStr();
						String dispMaxStr = effectiveMaxToStr();
						if (dispMinStr.length() > DISP_MIN_MAX_CHAR_COUNT)
							dispMinStr = dispMinStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
						if (dispMaxStr.length() > DISP_MIN_MAX_CHAR_COUNT)
							dispMaxStr = dispMaxStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
						dispMinLabel.setText("Disp Min: " + dispMinStr);
						dispMaxLabel.setText("Disp Max: " + dispMaxStr);
						pz.draw();
						frame.repaint();
						return true;
					}
				};
				worker.execute();
			}
		});
		winSize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog dlg = new JDialog(frame, "", Dialog.ModalityType.DOCUMENT_MODAL);
				dlg.getContentPane().setLayout(new BoxLayout(dlg.getContentPane(), BoxLayout.Y_AXIS));
				dlg.add(new JLabel("Choose width and height of new data windows"));
				dlg.add(new JLabel("Width (maximum of 1024)"));
				JTextField widthField = new JTextField(8);
				widthField.setText(""+desiredWidth);
				dlg.add(widthField);
				dlg.add(new JLabel("Height (maximum of 1024)"));
				JTextField heightField = new JTextField(8);
				heightField.setText(""+desiredHeight);
				dlg.add(heightField);
				JButton ok = new JButton("Ok");
				dlg.add(ok);
				JButton cancel = new JButton("Cancel");
				dlg.add(cancel);
				ok.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						dlg.setVisible(false);
						int width;
						int height;
						String widthStr = widthField.getText();
						if (widthStr != null && widthStr.length() > 0) {
							try { 
								width = Integer.parseInt(widthStr);
							} catch (NumberFormatException exc) {
								return;
							}
							if (width < 1 || width > 1024) return;
						}
						else
							return;
						String heightStr = heightField.getText();
						if (heightStr != null && heightStr.length() > 0) {
							try { 
								height = Integer.parseInt(heightStr);
							} catch (NumberFormatException exc) {
								return;
							}
							if (height < 1 || height > 1024) return;
						}
						else
							return;
						desiredWidth = width;
						desiredHeight = height;
					}					
				});
				cancel.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						dlg.setVisible(false);
					}
				});				
				dlg.setVisible(true);
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
				dlg.setLocationByPlatform(true);
				dlg.getContentPane().setLayout(new BoxLayout(dlg.getContentPane(), BoxLayout.Y_AXIS));
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
				String[] axes = new String[dataSource.numDimensions()];
				for (int i = 0; i < axes.length; i++) {
					String label = dataSource.getAxisType(i);
					axes[i] = label;
				}
				int axis = JOptionPane.showOptionDialog(frame, "Choose an axis to explode along", "Axis chooser",
						JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, axes, null);
				if (axis >= 0 && axis < dataSource.numDimensions())
					explode(dataSource, axis);
			}
		});
		fft.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				DimensionedDataSource<U> data = grabCurrentPlane();
				
				if (min instanceof Float16Member) {
					doFFT(G.CHLF, G.HLF, data, constructionLabel);
				}
				else if (min instanceof Float32Member) {
					doFFT(G.CFLT, G.FLT, data, constructionLabel);
				}
				else if (min instanceof Float64Member) {
					doFFT(G.CDBL, G.DBL, data, constructionLabel);
				}
				else if (min instanceof Float128Member) {
					doFFT(G.CQUAD, G.QUAD, data, constructionLabel);
				}
				else if (min instanceof HighPrecisionMember) {
					doFFT(G.CHP, G.HP, data, constructionLabel);
				}
				else
					JOptionPane.showMessageDialog(frame,
						    "Image is not in a recognizable floating point format.",
						    "WARNING",
						    JOptionPane.WARNING_MESSAGE);
			}
		});
		
		transform.addActionListener(new ActionListener() {
			
			private JTextField constant = new JTextField("<Enter constant here>");
			private Procedure2<U,U> xform = null;
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog dlg = new JDialog(frame, "", Dialog.ModalityType.DOCUMENT_MODAL);
				dlg.setLocationByPlatform(true);
				dlg.getContentPane().setLayout(new BoxLayout(dlg.getContentPane(), BoxLayout.Y_AXIS));
				dlg.add(constant);
				dlg.add(new JLabel("Choose operation"));
				ButtonGroup bg = new ButtonGroup();
				JRadioButton add = new JRadioButton("Add");
				JRadioButton sub = new JRadioButton("Subtract");
				JRadioButton mul = new JRadioButton("Multiply");
				JRadioButton div = new JRadioButton("Divide");
				JRadioButton pow = new JRadioButton("Power");
				JRadioButton fill = new JRadioButton("Fill");
				JRadioButton invert = new JRadioButton("Invert");
				JRadioButton sqrt = new JRadioButton("Sqrt");
				JRadioButton sqr = new JRadioButton("Square");
				JRadioButton log = new JRadioButton("Log");
				JRadioButton exp = new JRadioButton("Exp");
				JRadioButton sin = new JRadioButton("Sin");
				JRadioButton cos = new JRadioButton("Cos");
				JRadioButton tan = new JRadioButton("Tan");
				JRadioButton sinh = new JRadioButton("Sinh");
				JRadioButton cosh = new JRadioButton("Cosh");
				JRadioButton tanh = new JRadioButton("Tanh");
				bg.add(add);
				bg.add(sub);
				bg.add(mul);
				bg.add(div);
				bg.add(pow);
				bg.add(fill);
				bg.add(invert);
				bg.add(sqrt);
				bg.add(sqr);
				bg.add(log);
				bg.add(exp);
				bg.add(sin);
				bg.add(cos);
				bg.add(tan);
				bg.add(sinh);
				bg.add(cosh);
				bg.add(tanh);
				dlg.add(add);
				dlg.add(sub);
				dlg.add(mul);
				dlg.add(div);
				dlg.add(pow);
				dlg.add(fill);
				dlg.add(invert);
				dlg.add(sqrt);
				dlg.add(sqr);
				dlg.add(log);
				dlg.add(exp);
				dlg.add(sin);
				dlg.add(cos);
				dlg.add(tan);
				dlg.add(sinh);
				dlg.add(cosh);
				dlg.add(tanh);
				
				class ADD<A extends Algebra<A,U> & Addition<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								U cons = a.construct(constant.getText());
								a.add().call(in, cons, out);
							}
						};
					}
				};
				add.addActionListener(new ADD());

				class SUB<A extends Algebra<A,U> & Addition<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								U cons = a.construct(constant.getText());
								a.subtract().call(in, cons, out);
							}
						};
					}
				};
				sub.addActionListener(new SUB());

				class MUL<A extends Algebra<A,U> & Multiplication<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								U cons = a.construct(constant.getText());
								a.multiply().call(in, cons, out);
							}
						};
					}
				};
				mul.addActionListener(new MUL());

				class DIV<A extends Algebra<A,U> & Invertible<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								U cons = a.construct(constant.getText());
								a.divide().call(in, cons, out);
							}
						};
					}
				};
				div.addActionListener(new DIV());

				class POW<A extends Algebra<A,U> & Power<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								U cons = a.construct(constant.getText());
								a.pow().call(in, cons, out);
							}
						};
					}
				};
				pow.addActionListener(new POW());

				class FILL<A extends Algebra<A,U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								U cons = a.construct(constant.getText());
								a.assign().call(cons, out);
							}
						};
					}
				};
				fill.addActionListener(new FILL());
				
				class INV<A extends Algebra<A,U> & Power<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						//xform = new Procedure2<U,U>() {
						//	
						//	@Override
						//	public void call(U in, U out) {
						//		U cons = a.construct(constant.getText());
						//		a.pow().call(in, cons, out);
						//	}
						//};
					}
				};
				invert.addActionListener(new INV());
				
				class SQRT<A extends Algebra<A,U> & Roots<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								a.sqrt().call(in, out);
							}
						};
					}
				};
				sqrt.addActionListener(new SQRT());
				
				class SQR<A extends Algebra<A,U> & Multiplication<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								a.multiply().call(in, in, out);
							}
						};
					}
				};
				sqr.addActionListener(new SQR());
				
				class LOG<A extends Algebra<A,U> & Exponential<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								a.log().call(in, out);
							}
						};
					}
				};
				log.addActionListener(new LOG());
				
				class EXP<A extends Algebra<A,U> & Exponential<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								a.exp().call(in, out);
							}
						};
					}
				};
				exp.addActionListener(new EXP());
				
				class SIN<A extends Algebra<A,U> & Trigonometric<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								a.sin().call(in, out);
							}
						};
					}
				};
				sin.addActionListener(new SIN());

				class COS<A extends Algebra<A,U> & Trigonometric<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								a.cos().call(in, out);
							}
						};
					}
				};
				cos.addActionListener(new COS());
				
				class TAN<A extends Algebra<A,U> & Trigonometric<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								a.tan().call(in, out);
							}
						};
					}
				};
				tan.addActionListener(new TAN());
				
				class SINH<A extends Algebra<A,U> & Hyperbolic<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								a.sinh().call(in, out);
							}
						};
					}
				};
				sinh.addActionListener(new SINH());

				class COSH<A extends Algebra<A,U> & Hyperbolic<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								a.cosh().call(in, out);
							}
						};
					}
				};
				cosh.addActionListener(new COSH());
				
				class TANH<A extends Algebra<A,U> & Hyperbolic<U>> implements ActionListener {

					A a = (A) alg;
					public void actionPerformed(ActionEvent e) {
					
						xform = new Procedure2<U,U>() {
							
							@Override
							public void call(U in, U out) {
								a.tanh().call(in, out);
							}
						};
					}
				};
				tanh.addActionListener(new TANH());
				
				
				
				JButton ok = new JButton("Ok");
				ok.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						dlg.setVisible(false);
					}
				});
				dlg.add(ok);
				dlg.pack();
				dlg.setVisible(true);
				if (xform != null) {
					Transform2.compute(alg, xform, planeData.getDataSource().rawData(), planeData.getDataSource().rawData());
				}
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
			JButton stopButton = new JButton("Stop");
			JButton chooseButton = new JButton("Choose ...");
			int axisPos = i;
			long maxVal = planeData.getDataSourceAxisSize(i);
			positionLabels[i].setText(""+(planeData.getPositionValue(i)+1)+" / "+maxVal);
			int pos = planeData.getDataSourceAxisNumber(i);
			String axisLabel = planeData.getDataSource().getAxisType(pos) + " : ";
			JLabel jax = new JLabel(axisLabel);
			jax.setFont(font);
			miniPanel.add(jax);
			miniPanel.add(positionLabels[i]);
			miniPanel.add(homeButton);
			miniPanel.add(decrementButton);
			miniPanel.add(incrementButton);
			miniPanel.add(endButton);
			miniPanel.add(animButton);
			miniPanel.add(stopButton);
			miniPanel.add(chooseButton);
			positions.add(miniPanel);
			decrementButton.addActionListener(new Decrementer(i));
			incrementButton.addActionListener(new Incrementer(i));
			homeButton.addActionListener(new Home(i));
			endButton.addActionListener(new End(i));
			animButton.addActionListener(new Animator(i));
			stopButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (animatingRightNow.get())
						pleaseQuitAnimating.set(true);
				}
			});
			chooseButton.addActionListener(new ActionListener() {

				boolean cancelled = false;
				
				@Override
				public void actionPerformed(ActionEvent e) {
					JDialog dlg = new JDialog(frame, "", Dialog.ModalityType.DOCUMENT_MODAL);
					dlg.setLocationByPlatform(true);
					dlg.getContentPane().setLayout(new BoxLayout(dlg.getContentPane(), BoxLayout.Y_AXIS));
					dlg.add(new JLabel("Choose value along axis to jump to"));
					JTextField valueField = new JTextField(20);
					valueField.setText("");
					dlg.add(valueField);
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
						String indexStr = valueField.getText();
						if (indexStr != null && indexStr.length() > 0) {
							long idx;
							try { 
								idx = Long.parseLong(indexStr);
							} catch (NumberFormatException exc) {
								return;
							}
							if (idx < 1) idx = 1;
							if (idx > maxVal) idx = maxVal;
							planeData.setPositionValue(axisPos, idx-1);
							positionLabels[axisPos].setText(""+(idx)+" / "+maxVal);
							pz.draw();
							frame.repaint();
						}
					}
				}
			});
		}

		JLabel readout = new JLabel();
		readout.setBackground(Color.WHITE);
		readout.setOpaque(true);
		readout.setText("");
		readout.setFont(font);
		scrollPane.addMouseMotionListener(new MouseMotionListener() {

			HighPrecisionMember hpVal = G.HP.construct();
			long[] modelCoords = new long[dataSource.numDimensions()];
			BigDecimal[] realWorldCoords = new BigDecimal[dataSource.numDimensions()]; 
			U value = alg.construct();
					
			@Override
			public void mouseMoved(MouseEvent e) {
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
					sb.append(dataSource.getAxisType(axisNumber0));
					sb.append(" = ");
					sb.append(i0);
					// only display calibrated values if they are not == 1.0 * uncalibrated values
					if (axisNumber0 < dataSource.numDimensions()) {
						if (realWorldCoords[axisNumber0].subtract(BigDecimal.valueOf(modelCoords[axisNumber0])).abs().compareTo(BigDecimal.valueOf(0.000001)) > 0) {
							sb.append(" (");
							sb.append(df.format(realWorldCoords[axisNumber0]));
							sb.append(" ");
							sb.append(dataSource.getAxisUnit(axisNumber0));
							sb.append(")");
						}
					}
					if (axisNumber1 < dataSource.numDimensions()) {
						sb.append(", ");
						sb.append(dataSource.getAxisType(axisNumber1));
						sb.append("= ");
						sb.append(i1);
					}
					// only display calibrated values if they are not == 1.0 * uncalibrated values
					if (axisNumber1 < dataSource.numDimensions()) {
						if (realWorldCoords[axisNumber1].subtract(BigDecimal.valueOf(modelCoords[axisNumber1])).abs().compareTo(BigDecimal.valueOf(0.000001)) > 0) {
							sb.append(" (");
							sb.append(df.format(realWorldCoords[axisNumber1]));
							sb.append(" ");
							sb.append(dataSource.getAxisUnit(axisNumber1));
							sb.append(")");
						}
					}
					sb.append(", value = ");
					if (alternateValue != null)
						sb.append(alternateValue);
					else
						sb.append(df.format(hpVal.v()));
					sb.append(" ");
					sb.append(dataSource.getValueUnit());
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
				setMinMax();
				String minStr = min.toString();
				String maxStr = max.toString();
				String dispMinStr = effectiveMinToStr();
				String dispMaxStr = effectiveMaxToStr();
				if (minStr.length() > MIN_MAX_CHAR_COUNT)
					minStr = minStr.substring(0,MIN_MAX_CHAR_COUNT) + "...";
				if (maxStr.length() > MIN_MAX_CHAR_COUNT)
					maxStr = maxStr.substring(0,MIN_MAX_CHAR_COUNT) + "...";
				if (dispMinStr.length() > DISP_MIN_MAX_CHAR_COUNT)
					dispMinStr = dispMinStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
				if (dispMaxStr.length() > DISP_MIN_MAX_CHAR_COUNT)
					dispMaxStr = dispMaxStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
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

		findMinsAndMaxes(alg, dataMn, dataMx);
		
		setMinMax();

		String minStr = min.toString();
		String maxStr = max.toString();
		String dispMinStr = effectiveMinToStr();
		String dispMaxStr = effectiveMaxToStr();
		if (minStr.length() > MIN_MAX_CHAR_COUNT)
			minStr = minStr.substring(0,MIN_MAX_CHAR_COUNT) + "...";
		if (maxStr.length() > MIN_MAX_CHAR_COUNT)
			maxStr = maxStr.substring(0,MIN_MAX_CHAR_COUNT) + "...";
		if (dispMinStr.length() > DISP_MIN_MAX_CHAR_COUNT)
			dispMinStr = dispMinStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
		if (dispMaxStr.length() > DISP_MIN_MAX_CHAR_COUNT)
			dispMaxStr = dispMaxStr.substring(0,DISP_MIN_MAX_CHAR_COUNT) + "...";
		minLabel.setText("Min: " + minStr);
		maxLabel.setText("Max: " + maxStr);
		dispMinLabel.setText("Disp Min: " + dispMinStr);
		dispMaxLabel.setText("Disp Max: " + dispMaxStr);

		pz.draw();
		
		frame.repaint();
	}

	private BigDecimal actualMin() {
		HighPrecisionMember tmp = G.HP.construct();
		((HighPrecRepresentation)min).toHighPrec(tmp);
		return tmp.v();
	}
	
	private BigDecimal actualMax() {
		HighPrecisionMember tmp = G.HP.construct();
		((HighPrecRepresentation)max).toHighPrec(tmp);
		return tmp.v();
	}
	
	private BigDecimal effectiveMin() {
		if (dispMin == null)
			return actualMin();
		return dispMin.v();
	}

	private BigDecimal effectiveMax() {
		if (dispMax == null)
			return actualMax();
		return dispMax.v();
	}
	
	private String effectiveMinToStr() {
		return effectiveMin().toString();
	}
	
	private String effectiveMaxToStr() {
		return effectiveMax().toString();
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
	
	@SuppressWarnings({"rawtypes","unchecked"})
	public DimensionedDataSource<U> grabCurrentPlane() {
		int axisNumber0 = planeData.axisNumber0();
		int axisNumber1 = planeData.axisNumber1();
		
		long dimX = planeData.d0();
		long dimY = planeData.d1();
		
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

		String d0Str = axisNumber0 < origDs.numDimensions() ? origDs.getAxisType(axisNumber0) : "d0";
		String d1Str = axisNumber1 < origDs.numDimensions() ? origDs.getAxisType(axisNumber1) : "d1";
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

		newDs.setName(origDs.getName().length() == 0 ? miniTitle : (miniTitle + " of "+origDs.getName()));
		if (axisNumber0 < origDs.numDimensions()) {
			newDs.setAxisType(0, origDs.getAxisType(axisNumber0));
			newDs.setAxisUnit(0, origDs.getAxisUnit(axisNumber0));
		}
		if (axisNumber1 < origDs.numDimensions()) {
			newDs.setAxisType(1, origDs.getAxisType(axisNumber1));
			newDs.setAxisUnit(1, origDs.getAxisUnit(axisNumber1));
		}
		newDs.setValueType(origDs.getValueType());
		newDs.setValueUnit(origDs.getValueUnit());
	
		CoordinateSpace origSpace = planeData.getDataSource().getCoordinateSpace();
		if (origSpace instanceof LinearNdCoordinateSpace) {

			LinearNdCoordinateSpace origLinSpace = (LinearNdCoordinateSpace) origSpace;
		
			BigDecimal[] scales = new BigDecimal[2];

			scales[0] = origLinSpace.getScale(axisNumber0);
			if (axisNumber1 < origDs.numDimensions())
				scales[1] = origLinSpace.getScale(axisNumber1);
			else
				scales[1] = BigDecimal.ONE;
				
		
			BigDecimal[] offsets = new BigDecimal[2];
		
			offsets[0] = origLinSpace.getOffset(axisNumber0);
			if (axisNumber1 < origDs.numDimensions())
				offsets[1] = origLinSpace.getOffset(axisNumber1);
			else
				offsets[1] = BigDecimal.ZERO;
		
			long[] coord = new long[origDs.numDimensions()];
		
			offsets[0] = origLinSpace.project(coord, axisNumber0);
			offsets[1] = origLinSpace.project(coord, axisNumber1);

			LinearNdCoordinateSpace newLinSpace = new LinearNdCoordinateSpace(scales, offsets);
		
			newDs.setCoordinateSpace(newLinSpace);
		}

		return newDs;
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

			SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

				@Override
				protected Object doInBackground() throws Exception {
					long maxVal = planeData.getDataSourceAxisSize(extraPos);
					for (long i = 0; i < maxVal; i++) {
						if (pleaseQuitAnimating.get()) {
							pleaseQuitAnimating.getAndSet(false);
							animatingRightNow.set(false);
							return true;
						}
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
					pleaseQuitAnimating.getAndSet(false);
					animatingRightNow.set(false);
					return true;
				}
				
			};
			
			worker.execute();
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
		if (axisNumber1 >= model.numDimensions())
			ctrYLabel.setText("Zoom Ctr d1: 0");
		else
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
	
	private void setMinMax() {

		if (preferDataRange) {
			
			alg.assign().call(dataMin, min);
			alg.assign().call(dataMax, max);
		}
		else {
			
			alg.assign().call(typeMin, min);
			alg.assign().call(typeMax, max);
		}
	}
	
	private void findMinsAndMaxes(T alg, U dataMn, U dataMx) {

		if (dataMn == null || dataMx == null)
			pixelDataBounds(alg, planeData.getDataSource().rawData(), dataMin, dataMax);
		else {
			// Avoid rescanning the WHOLE dataset to find values we already know. On a
			// very large dataset this can save minutes.
			alg.assign().call(dataMn, dataMin);
			alg.assign().call(dataMx, dataMax);
		}
		pixelTypeBounds(alg, typeMin, typeMax);
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
			// old way
			//return scaleDenom / 2;  // this works well when we only support odd zoom factors
			
			// new way: much much faster
			return 0;
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
			
			// paint the pixels into the plane of data
			
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
			
			// now paint a yellow outline around the image boundaries
			
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

			BigDecimal numV;
			if (numValues <= 0)
				numV = BigDecimal.ONE;
			else
				numV = BigDecimal.valueOf(numValues);
			
			BigDecimal average =
					valueSum.v().divide(numV, HighPrecisionAlgebra.getContext());
			
			BigDecimal numer = average.subtract(hpMin.v());
			
			BigDecimal denom = hpMax.v().subtract(hpMin.v());
			
			if (denom.compareTo(BigDecimal.ZERO) == 0) {
				denom = BigDecimal.ONE;
			}
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
		 * Current pan position, zoom level, and lut colors are
		 * preserved.
		 */
		@SuppressWarnings("rawtypes")
		public DimensionedDataSource<ArgbMember> takeSnapshot()
		{
			int dimX = paneWidth;
			int dimY = paneHeight;
			
			DimensionedDataSource<ArgbMember> newDs = (DimensionedDataSource<ArgbMember>)
					DimensionedStorage.allocate((Allocatable) G.ARGB.construct(), new long[] {dimX, dimY});
			
			TwoDView<ArgbMember> view = new TwoDView<>(newDs);
			
			// Safe cast as img is of correct type 
			
			DataBufferInt buffer = (DataBufferInt) argbData.getRaster().getDataBuffer();

			// Conveniently, the buffer already contains the data array
			
			int[] arrayInt = buffer.getData();
			
			ArgbMember tmp = G.ARGB.construct();
			for (int y = 0; y < dimY; y++) {
				final int rowPos = y * dimX;
				for (int x = 0; x < dimX; x++) {
					int argb = arrayInt[rowPos + x];
					tmp.setA(RgbUtils.a(argb));
					tmp.setR(RgbUtils.r(argb));
					tmp.setG(RgbUtils.g(argb));
					tmp.setB(RgbUtils.b(argb));
					view.set(x, y, tmp);
				}
			}
			
			return newDs;
		}
	}
	
	public <CA extends Algebra<CA,C> & Addition<C> & Multiplication<C> & Conjugate<C>,
			C extends SetComplex<R> & GetComplex<R> & Allocatable<C>,
			RA extends Algebra<RA,R> & Trigonometric<R> & RealConstants<R> &
				Multiplication<R> & Addition<R> & Invertible<R> & Unity<R> &
				NaN<R> & InverseTrigonometric<R> & Roots<R> & Ordered<R>,
			R extends Allocatable<R>>
		void doFFT(Algebra<?,?> complexAlgebra, Algebra<?,?> realAlgebra, DimensionedDataSource<?> input, JLabel theLabel)
	{
		SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
	
			@Override
			protected Object doInBackground() throws Exception {
		
				String error = null;
				
				if (!(complexAlgebra instanceof Addition))
					error = "Complex algebra does not implement Addition";
				
				else if (!(complexAlgebra instanceof Multiplication))
					error = "Complex algebra does not implement Multiplication";
				
				@SuppressWarnings("unchecked")
				CA cmplxAlg = (CA) complexAlgebra;
				
				C tmpC = cmplxAlg.construct();
				
				if (!(tmpC instanceof SetComplex))
					error = "Complex number does not implement SetComplex";
				
				else if (!(tmpC instanceof GetComplex))
					error = "Complex number does not implement GetComplex";
				
				else if (!(tmpC instanceof Allocatable))
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
				RA realAlg = (RA) realAlgebra;
				
				R tmpR = realAlg.construct();
				
				try {
					
					tmpC.setR(tmpR);
					tmpC.setI(tmpR);
					
				} catch (ClassCastException e) {
					
					error = "Complex algebra and real algebra are not compatible";
				}
				
				try {
					
					tmpC.getR(tmpR);
					tmpC.getI(tmpR);
					
				} catch (ClassCastException e) {
					
					error = "Complex algebra and real algebra are not compatible";
				}
				
				if (error != null) {
					JOptionPane.showMessageDialog(frame,
						    "FFT error: ."+error,
						    "WARNING",
						    JOptionPane.WARNING_MESSAGE);
					return false;
				}

				@SuppressWarnings("unchecked")
				DimensionedDataSource<R> realData = (DimensionedDataSource<R>) input;
				
				long[] dims = new long[2];
				for (int i = 0; i < dims.length; i++) {
					dims[i] = realData.dimension(i);
				}
				
				DimensionedDataSource<C> complexData = DimensionedStorage.allocate(cmplxAlg.construct(), dims);
				
				R realValue = realAlg.construct();
				R imagValue = realAlg.construct();
				C complexValue = cmplxAlg.construct();
				
				TwoDView<R> realVw = new TwoDView<>(realData);
				TwoDView<C> complexVw = new TwoDView<>(complexData);
				
				for (long y = 0; y < dims[1]; y++) {
					for (long x = 0; x < dims[0]; x++) {
						realVw.get(x, y, realValue);
						complexValue.setR(realValue);
						complexVw.set(x, y, complexValue);
					}
				}
				
				DimensionedDataSource<C> result = FFT2D.compute(cmplxAlg, realAlg, complexData);
				
				long sz = result.dimension(0);

				// swap quadrants: this should be optional and maybe part of FFT code
				
				C tmp1 = cmplxAlg.construct();
				C tmp2 = cmplxAlg.construct();
				
				TwoDView<C> vw = new TwoDView<C>(result);
				
				long quadSize = sz/2;
				
				// swap ul and lr
				for (long y = 0; y < quadSize; y++) {
					for (long x = 0; x < quadSize; x++) {
						vw.get(x, y, tmp1);
						vw.get(x+quadSize, y+quadSize, tmp2);
						vw.set(x+quadSize, y+quadSize, tmp1);
						vw.set(x, y, tmp2);
					}
				}
				
				// swap ur and ll

				for (long y = 0; y < quadSize; y++) {
					for (long x = quadSize; x < sz; x++) {
						vw.get(x, y, tmp1);
						vw.get(x-quadSize, y+quadSize, tmp2);
						vw.set(x-quadSize, y+quadSize, tmp1);
						vw.set(x, y, tmp2);
					}
				}
				
//				DimensionedDataSource<R> magDs = DimensionedStorage.allocate(realValue, new long[] {sz,sz});

//				R mag = realAlg.construct();
//				for (long i = 0; i < sz*sz; i++) {
//					result.rawData().get(i, tmpC);
//					tmpC.getR(realValue);
//					tmpC.getI(imagValue);
//					PolarCoords.magnitude(realAlg, realValue, imagValue, mag);
//					magDs.rawData().set(i, mag);
//				}
//				magDs.setCoordinateSpace(new Polar2dCoordinateSpace(BigDecimal.valueOf(1), BigDecimal.valueOf(Math.PI / 512.0)));

				IndexedDataSource<R> m = Storage.allocate(realValue, sz*sz);
				IndexedDataSource<R> p = Storage.allocate(realValue, sz*sz);
				
				GetRValues.compute(cmplxAlg, realAlg, result.rawData(), m);
				GetIValues.compute(cmplxAlg, realAlg, result.rawData(), p);
				
				R mag = realAlg.construct();
				R phas = realAlg.construct();
				for (long i = 0; i < sz; i++) {
					m.get(i, realValue);
					p.get(i, imagValue);
					PolarCoords.magnitude(realAlg, realValue, imagValue, mag);
					PolarCoords.phase(realAlg, realValue, imagValue, phas);
					m.set(i, mag);
					p.set(i, phas);
				}
		
				DimensionedDataSource<R> magDs = new NdData<R>(new long[] {sz, sz}, m);
		
				DimensionedDataSource<R> phasDs = new NdData<R>(new long[] {sz, sz}, p);
		
				magDs.setName("Magnitudes of FFT of "+input.getName());
				
				phasDs.setName("Phases of FFT of "+input.getName());
		
				new RealImageViewer<>(realAlg, magDs);
		
				new RealImageViewer<>(realAlg, phasDs);

				/* Nice FFT/InvFFT debugging code
				
				DimensionedDataSource<C> result2 = InvFFT2D.compute(cmplxAlg, realAlg, result);

				IndexedDataSource<R> re = Storage.allocate(realValue, sz*sz);
				IndexedDataSource<R> im = Storage.allocate(realValue, sz*sz);

				GetRValues.compute(cmplxAlg, realAlg, result2.rawData(), re);
				GetIValues.compute(cmplxAlg, realAlg, result2.rawData(), im);

				DimensionedDataSource<R> reals =
						new NdData<R>(new long[] {sz, sz}, re);
		
				DimensionedDataSource<R> imags =
						new NdData<R>(new long[] {sz, sz}, im);

				reals.setName("Real values of InvFFT of FFT xformed data");

				imags.setName("Imag values of InvFFT of FFT xformed data");

				new RealImageViewer<>(realAlg, reals);

				new RealImageViewer<>(realAlg, imags);
				
				*/
				return true;
			}

/* Support this instead of real viewers someday

				DimensionedDataSource<M> complexDs =
						new NdData<M>(new long[] {edgeSize, edgeSize}, complexOutput);

				complexDs.setName("FFT of "+input.getName());

				complexDs.setSource(input.getSource());

				new ComplexImageViewer<L,M,N,O>(cmplxAlg, realAlg, complexDs);
*/
			
			@Override
			protected void done() {

				// here I am in a background thread but I will tell the event
				//   thread to update it's control after exit here (or is it
				//   right when setIcon() is invoked?)

				theLabel.setIcon(null);

				super.done();
			}
		};

		// here I am on the event thread hopefully it will update right away
		
		ImageIcon tmp = new ImageIcon(ICON_URL);
		Image image = tmp.getImage();
		Image scaledImage = image.getScaledInstance(80, 80, java.awt.Image.SCALE_DEFAULT);
		theLabel.setIcon(new ImageIcon(scaledImage));
		
		// here I start a new background thread
		
		worker.execute();
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

		IndexedDataSource<O> inList = input.rawData();  
		IndexedDataSource<M> outList = output.rawData();

		O in  = inAlg.construct();
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
		List<DimensionedDataSource<W>> results = NdSplit.compute(enhancedAlg, axis, 1L, ds);
		for (DimensionedDataSource<W> dataset : results) {
			new RealImageViewer<V,W>(enhancedAlg, dataset);
		}
	}
}
