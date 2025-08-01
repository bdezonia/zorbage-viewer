/*
 * zorbage-viewer: utility app for loading and viewing various image data formats
 *
 * Copyright (c) 2020-2022 Barry DeZonia All rights reserved.
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
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algebra.Allocatable;
import nom.bdezonia.zorbage.algebra.G;
import nom.bdezonia.zorbage.algebra.SetFromDoubles;
import nom.bdezonia.zorbage.algorithm.NdSplit;
import nom.bdezonia.zorbage.coordinates.CoordinateSpace;
import nom.bdezonia.zorbage.coordinates.LinearNdCoordinateSpace;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.data.DimensionedStorage;
import nom.bdezonia.zorbage.datasource.IndexedDataSource;
import nom.bdezonia.zorbage.dataview.PlaneView;
import nom.bdezonia.zorbage.dataview.TwoDView;
import nom.bdezonia.zorbage.misc.DataSourceUtils;
import nom.bdezonia.zorbage.tuple.Tuple2;
import nom.bdezonia.zorbage.type.color.ArgbAlgebra;
import nom.bdezonia.zorbage.type.color.ArgbMember;
import nom.bdezonia.zorbage.type.color.RgbAlgebra;
import nom.bdezonia.zorbage.type.color.RgbMember;
import nom.bdezonia.zorbage.type.color.RgbUtils;

/**
 * 
 * @author Barry DeZonia
 *
 */
public class RgbColorImageViewer<T extends Algebra<T,U>, U> {

	private static int desiredWidth = 1024;
	private static int desiredHeight = 1024;
	
	private static MathContext roundContext = new MathContext(6);
	
	private final T alg;
	private final PlaneView<U> planeData;
	private final PanZoomView pz;
	private final BufferedImage argbData;
	private final JLabel[] positionLabels;
	private final JFrame frame;
	private final Font font = new Font("Verdana", Font.PLAIN, 18);
	private AtomicBoolean animatingRightNow = new AtomicBoolean();
	private AtomicBoolean pleaseQuitAnimating = new AtomicBoolean();
	private final JLabel ctrXLabel; 
	private final JLabel ctrYLabel; 
	DecimalFormat df = new DecimalFormat("0.00000");

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
		this.pz = new PanZoomView(desiredWidth, desiredHeight);

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
		JLabel c = new JLabel("Data unit: " + (valueUnit.equals("") ? "<none>" : valueUnit));
		a.setFont(font);
		b.setFont(font);
		c.setFont(font);
		box.add(a);
		box.add(b);
		box.add(c);
		headerPanelLeft.add(box);
		
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BorderLayout());
		headerPanel.add(headerPanelLeft, BorderLayout.WEST);

		JPanel graphicsPanel = new JPanel();
		JLabel image = new JLabel(new ImageIcon(argbData));
		JScrollPane scrollPane = new JScrollPane(image);
		graphicsPanel.add(scrollPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton metadata = new JButton("Metadata ...");
		JButton swapAxes = new JButton("Swap Axes");
		JButton snapshot = new JButton("Snapshot");
		JButton grabPlane = new JButton("Grab Plane");
		JButton incZoom = new JButton("Zoom In");
		JButton decZoom = new JButton("Zoom Out");
		JButton panLeft = new JButton("Pan Left");
		JButton panRight = new JButton("Pan Right");
		JButton panUp = new JButton("Pan Up");
		JButton panDown = new JButton("Pan Down");
		JButton resetZoom = new JButton("Reset Pan/Zoom");
		JButton toFloat = new JButton("To Float ...");
		JButton explode = new JButton("Explode ...");
		JButton saveAs = new JButton("Save As ...");
		Dimension size = new Dimension(150, 40);
		metadata.setMinimumSize(size);
		incZoom.setMinimumSize(size);
		decZoom.setMinimumSize(size);
		panLeft.setMinimumSize(size);
		panRight.setMinimumSize(size);
		panUp.setMinimumSize(size);
		panDown.setMinimumSize(size);
		resetZoom.setMinimumSize(size);
		toFloat.setMinimumSize(size);
		snapshot.setMinimumSize(size);
		grabPlane.setMinimumSize(size);
		swapAxes.setMinimumSize(size);
		explode.setMinimumSize(size);
		saveAs.setMinimumSize(size);
		metadata.setMaximumSize(size);
		incZoom.setMaximumSize(size);
		decZoom.setMaximumSize(size);
		panLeft.setMaximumSize(size);
		panRight.setMaximumSize(size);
		panUp.setMaximumSize(size);
		panDown.setMaximumSize(size);
		resetZoom.setMaximumSize(size);
		toFloat.setMaximumSize(size);
		snapshot.setMaximumSize(size);
		grabPlane.setMaximumSize(size);
		swapAxes.setMaximumSize(size);
		explode.setMaximumSize(size);
		saveAs.setMaximumSize(size);
		Box vertBox = Box.createVerticalBox();
		vertBox.add(metadata);
		vertBox.add(incZoom);
		vertBox.add(decZoom);
		vertBox.add(panLeft);
		vertBox.add(panRight);
		vertBox.add(panUp);
		vertBox.add(panDown);
		vertBox.add(resetZoom);
		vertBox.add(toFloat);
		vertBox.add(snapshot);
		vertBox.add(grabPlane);
		vertBox.add(swapAxes);
		vertBox.add(explode);
		vertBox.add(saveAs);
		buttonPanel.add(vertBox);
		metadata.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {

				@SuppressWarnings("unchecked")
				Tuple2<String, String>[] data = (Tuple2<String, String>[]) new Tuple2[0];
				
				data = dataSource.metadata().keySet().toArray(data);
				
				Arrays.sort(data, new Comparator<Tuple2<String, String>>() {
					
					@Override
					public int compare(Tuple2<String, String> first, Tuple2<String, String> second) {
						
						return first.a().compareTo(second.a());
					}
				});
				
				System.out.println("Metadata follows:");

				for (Tuple2<String, String> key : data) {
					
					System.out.println("  key: (" + key.a() + ")  type: (" + key.b() + ")");
				}
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
						new RgbColorImageViewer<>(alg, dataSource, i0, i1);
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

				new RgbColorImageViewer<>(alg, plane);
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
		toFloat.addActionListener(new ActionListener() {

			boolean cancelled = false;
			Algebra<?,?> fltAlg = null;
			
			@SuppressWarnings({"rawtypes", "unchecked"})
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

					convertToFloat((Algebra)fltAlg, alg, planeData.getDataSource());
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
		saveAs.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				new ImageSaver(frame, argbData).save();
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
			CoordinateSpace space = planeData.getDataSource().getCoordinateSpace();	
			BigDecimal[] currCoords = new BigDecimal[space.numDimensions()];
			getCurrCoords(space, currCoords);
			positionLabels[axisPos].setText(""+(planeData.getPositionValue(i)+1)+" / "+maxVal+" ("+currCoords[axisPos].round(roundContext)+")");
			int pos = planeData.getDataSourceAxisNumber(i);
			String axisLabel = planeData.getDataSource().getAxisType(pos) + " : ";
			JLabel jax = new JLabel(axisLabel);
			jax.setFont(font);
			miniPanel.add(homeButton);
			miniPanel.add(decrementButton);
			miniPanel.add(incrementButton);
			miniPanel.add(endButton);
			miniPanel.add(animButton);
			miniPanel.add(stopButton);
			miniPanel.add(chooseButton);
			miniPanel.add(jax);
			miniPanel.add(positionLabels[i]);
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
							CoordinateSpace space = planeData.getDataSource().getCoordinateSpace();
							BigDecimal[] currCoords = new BigDecimal[space.numDimensions()];
							getCurrCoords(space, currCoords);
							positionLabels[axisPos].setText(""+(idx)+" / "+maxVal+" ("+currCoords[axisPos].round(roundContext)+")");
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

			long[] modelCoords = new long[dataSource.numDimensions()];
			BigDecimal[] realWorldCoords = new BigDecimal[dataSource.numDimensions()]; 
			U value = alg.construct();
			RgbMember rgb = (value instanceof RgbMember) ? (RgbMember) value : null;
			ArgbMember argb = (value instanceof ArgbMember) ? (ArgbMember) value : null;

			@Override
			public void mouseMoved(MouseEvent e) {

				// model X and mouse X move in the same direction
				
				int pixelX = e.getX();

				// This weird math is because model "Y" runs bottom to
				// top but mouse coords run top to bottom.

				int pixelY = pz.paneHeight - e.getY() - 1;
				
				long i0 = pz.pixelToModel(pixelX, pz.getVirtualOriginX());

				long i1 = pz.pixelToModel(pixelY, pz.getVirtualOriginY());

				if (i0 >= 0 && i0 < planeData.d0() && i1 >= 0 && i1 < planeData.d1()) {
					planeData.getModelCoords(i0, i1, modelCoords);
					dataSource.getCoordinateSpace().project(modelCoords, realWorldCoords);
					planeData.get(i0, i1, value);
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
						sb.append(" = ");
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
					sb.append(dataSource.getValueUnit());
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
				CoordinateSpace space = planeData.getDataSource().getCoordinateSpace();
				BigDecimal[] currCoords = new BigDecimal[space.numDimensions()];
				getCurrCoords(space, currCoords);
				positionLabels[extraPos].setText(""+(pos+1)+" / "+maxVal+" ("+currCoords[extraPos].round(roundContext)+")");
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
				CoordinateSpace space = planeData.getDataSource().getCoordinateSpace();
				BigDecimal[] currCoords = new BigDecimal[space.numDimensions()];
				getCurrCoords(space, currCoords);
				positionLabels[extraPos].setText(""+(pos+1)+" / "+maxVal+" ("+currCoords[extraPos].round(roundContext)+")");
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
			CoordinateSpace space = planeData.getDataSource().getCoordinateSpace();
			BigDecimal[] currCoords = new BigDecimal[space.numDimensions()];
			getCurrCoords(space, currCoords);
			positionLabels[extraPos].setText(""+(1)+" / "+maxVal+" ("+currCoords[extraPos].round(roundContext)+")");
			pz.draw();
			frame.repaint();
		}
	}
	
	private void getCurrCoords(CoordinateSpace space, BigDecimal[] values) {
		long[] modelCoords = new long[space.numDimensions()];
		planeData.getModelCoords(0, 0, modelCoords);
		space.project(modelCoords, values);
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
			CoordinateSpace space = planeData.getDataSource().getCoordinateSpace();
			BigDecimal[] currCoords = new BigDecimal[space.numDimensions()];
			getCurrCoords(space, currCoords);
			positionLabels[extraPos].setText(""+(maxVal)+" / "+maxVal+" ("+currCoords[extraPos].round(roundContext)+")");
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
						CoordinateSpace space = planeData.getDataSource().getCoordinateSpace();
						BigDecimal[] currCoords = new BigDecimal[space.numDimensions()];
						getCurrCoords(space, currCoords);
						positionLabels[extraPos].setText(""+(i+1)+" / "+maxVal+" ("+currCoords[extraPos].round(roundContext)+")");
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

		// NOTE on panning:
		//   panLeft implementation mirrors panUp
		//   panRight implementation mirrors panDown
		//   but there is not 4 way symmetry.
		//   this is confusing and probably points out a bug.
		
		// Image smaller and bigger than pane
		//   - works at 1x, 1/3x, 3x
		
		public boolean panLeft(int numPixels) {
			
			if (pz.getVirtualOriginX() - numPixels < -(pz.getVirtualWidth()-1)) {
				return false;
			}
			
			originX -= pixelToModel(numPixels, 0);
			
			return true;
		}

		// Image smaller and bigger than pane
		//   - works at 1x, 1/3x, 3x

		public boolean panRight(int numPixels) {
			
			if (pz.getVirtualOriginX() + numPixels > (planeData.d0()-1)) {
				return false;
			}
			
			originX += pixelToModel(numPixels, 0);
			
			return true;
		}

		// Image smaller and bigger than pane
		//   - works at 1x, 1/3x, 3x
		//   - off by 1 or 2 pans when zoomed out

		public boolean panUp(int numPixels) {
			
			if (pz.getVirtualOriginY() - numPixels < -(pz.getVirtualHeight()-1)) {
				return false;
			}
			
			originY -= pixelToModel(numPixels, 0);
			
			return true;
		}

		// Image smaller and bigger than pane
		//   - works at 1x, 1/3x, 3x
		//   - off by 1 or 2 pans when zoomed out

		public boolean panDown(int numPixels) {
	
			if (pz.getVirtualOriginY() + numPixels > (planeData.d1()-1)) {
				return false;
			}
			
			originY += pixelToModel(numPixels, 0);
			
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
				long my = pixelToModel(y, originY);
				for (int x = 0; x < paneWidth; x++) {
					boolean modelCoordsInBounds = false;
					long mx = pixelToModel(x, originX);
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
					
					int boxHalfSize = drawingBoxHalfSize();
					for (int dy = -boxHalfSize; dy <= boxHalfSize; dy++) {
						int v = y + dy;
						for (int dx = -boxHalfSize; dx <= boxHalfSize; dx++) {
							int u = x + dx;
							// plot a point
							plot(color, arrayInt, u, v);
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

			BigInteger width = BigInteger.valueOf(paneWidth-1);
			
			if (pX0.compareTo(width) > 0 && pX1.compareTo(width) > 0)
				return;
			
			if (pY0.compareTo(BigInteger.ZERO) < 0 &&
					pY1.compareTo(BigInteger.ZERO) < 0)
				return;
			
			BigInteger height = BigInteger.valueOf(paneHeight-1);
			
			if (pY0.compareTo(height) > 0 && pY1.compareTo(height) > 0)
				return;
			
			// clip line if necessary
			
			if (pX0.compareTo(BigInteger.ZERO) < 0)
				pX0 = BigInteger.ZERO;

			if (pX1.compareTo(width) > 0)
				pX1 = width;

			if (pY0.compareTo(BigInteger.ZERO) < 0)
				pY0 = BigInteger.ZERO;

			if (pY1.compareTo(height) > 0)
				pY1 = height;
			
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
		 * Current pan position, and zoom level are preserved.
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
	
	<FA extends Algebra<FA,F>,
			F extends Allocatable<F> & SetFromDoubles,
			RGBA extends Algebra<RGBA, RGB>,
			RGB>
		void convertToFloat(FA fltAlg, RGBA rgbAlg, DimensionedDataSource<RGB> dataSource)
	{
		long[] dims = DataSourceUtils.dimensions(dataSource);
		
		DimensionedDataSource<F> fltDataSource = DimensionedStorage.allocate(fltAlg.construct(), dims);
		
		if (rgbAlg instanceof RgbAlgebra) {

			RgbAlgebra alg = (RgbAlgebra) rgbAlg;
			
			@SuppressWarnings("unchecked")
			IndexedDataSource<RgbMember> ds = (IndexedDataSource<RgbMember>) dataSource.rawData();

			IndexedDataSource<F> fltDs = (IndexedDataSource<F>) fltDataSource.rawData();

			RgbMember rgbVal = alg.construct();
			
			F fltVal = fltAlg.construct();
			
			for (long i = 0; i < ds.size(); i++) {
				
				ds.get(i, rgbVal);
				
				double intensity = RgbUtils.intensity(rgbVal.r(), rgbVal.g(), rgbVal.b());
				
				fltVal.setFromDoubles(intensity);

				fltDs.set(i, fltVal);
			}
		}
		else if (rgbAlg instanceof ArgbAlgebra) {
			
			ArgbAlgebra alg = (ArgbAlgebra) rgbAlg;
			
			@SuppressWarnings("unchecked")
			IndexedDataSource<ArgbMember> ds = (IndexedDataSource<ArgbMember>) dataSource.rawData();

			IndexedDataSource<F> fltDs = (IndexedDataSource<F>) fltDataSource.rawData();

			ArgbMember argbVal = alg.construct();
			
			F fltVal = fltAlg.construct();
			
			for (long i = 0; i < ds.size(); i++) {
				
				ds.get(i, argbVal);
				
				double intensity = RgbUtils.intensity(argbVal.a(), argbVal.r(), argbVal.g(), argbVal.b());
				
				fltVal.setFromDoubles(intensity);

				fltDs.set(i, fltVal);
			}
		}
		else {
			
			throw new IllegalArgumentException("ToFloat only works with ARGB or RGB data");
		}
		
		new RealImageViewer<FA,F>(fltAlg, fltDataSource);
	}
	
	@SuppressWarnings("unchecked")
	<V extends Algebra<V,W>, W extends Allocatable<W>>
		void explode(DimensionedDataSource<U> dataSource, int axis)
	{
		V enhancedAlg = (V) alg;
		DimensionedDataSource<W> ds = (DimensionedDataSource<W>) dataSource;
		List<DimensionedDataSource<W>>  results = NdSplit.compute(enhancedAlg, axis, 1L, ds);
		for (DimensionedDataSource<W> dataset : results) {
			new RgbColorImageViewer<V,W>(enhancedAlg, dataset);
		}
	}
}
