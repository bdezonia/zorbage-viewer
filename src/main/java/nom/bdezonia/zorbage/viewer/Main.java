/*
 * zorbage-viewer: utility app for loading and viewing various image data formats
 *
 * Copyright (C) 2020 Barry DeZonia
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *	this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	this list of conditions and the following disclaimer in the documentation
 *	and/or other materials provided with the distribution.
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
 */
package nom.bdezonia.zorbage.viewer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algebra.Bounded;
import nom.bdezonia.zorbage.algebra.G;
import nom.bdezonia.zorbage.algebra.HighPrecRepresentation;
import nom.bdezonia.zorbage.algebra.Ordered;
import nom.bdezonia.zorbage.algorithm.GridIterator;
import nom.bdezonia.zorbage.algorithm.MinMaxElement;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.datasource.IndexedDataSource;
import nom.bdezonia.zorbage.gdal.Gdal;
import nom.bdezonia.zorbage.netcdf.NetCDF;
import nom.bdezonia.zorbage.sampling.IntegerIndex;
import nom.bdezonia.zorbage.sampling.SamplingIterator;
import nom.bdezonia.zorbage.scifio.Scifio;
import nom.bdezonia.zorbage.tuple.Tuple2;
import nom.bdezonia.zorbage.type.float32.complex.ComplexFloat32Member;
import nom.bdezonia.zorbage.type.float64.complex.ComplexFloat64Member;
import nom.bdezonia.zorbage.type.highprec.real.HighPrecisionAlgebra;
import nom.bdezonia.zorbage.type.highprec.real.HighPrecisionMember;
import nom.bdezonia.zorbage.type.rgb.ArgbMember;
import nom.bdezonia.zorbage.type.rgb.RgbMember;

/**
 * 
 * @author Barry DeZonia
 *
 */
public class Main<T extends Algebra<T,U>, U> {

	private JFrame frame = null;
	
	private List<Tuple2<T,DimensionedDataSource<U>>> dataSources = new ArrayList<>();
	
	private int imgNumber = -1;

	private boolean preferDataRange = true;
	
	public static void main(String[] args) {

		Gdal.init();
		
		@SuppressWarnings("rawtypes")
		Main main = new Main();
		
		// Schedule a job for the event-dispatching thread: creating and showing
		// this application's GUI.
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				main.createAndShowGUI();
			}
		});
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private void createAndShowGUI() {

		//Make sure we have nice window decorations.
		JFrame.setDefaultLookAndFeelDecorated(true);
 
		//Create and set up the window.
		frame = new JFrame("Zorbage Data Viewer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
		JButton loadGdal = new JButton("Load using gdal");
		loadGdal.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			@SuppressWarnings("unchecked")
			public void mouseClicked(MouseEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.showOpenDialog(frame);
				File f = chooser.getSelectedFile();

				nom.bdezonia.zorbage.gdal.DataBundle gBundle =
						Gdal.loadAllDatasets(f.getAbsolutePath());
				
				for (DimensionedDataSource<?> source : gBundle.cdoubles) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.CDBL, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : gBundle.cfloats) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.CFLT, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : gBundle.doubles) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.DBL, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : gBundle.floats) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.FLT, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : gBundle.int16s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.INT16, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : gBundle.int32s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.INT32, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : gBundle.uint8s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT8, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : gBundle.uint16s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT16, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : gBundle.uint32s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT32, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				System.out.println("gdal files loaded");
			}
		});

		JButton loadNetcdf = new JButton("Load using netcdf");
		loadNetcdf.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			@SuppressWarnings("unchecked")
			public void mouseClicked(MouseEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.showOpenDialog(frame);
				File f = chooser.getSelectedFile();
				
				nom.bdezonia.zorbage.netcdf.DataBundle nBundle =
						NetCDF.loadAllDatasets(f.getAbsolutePath());
				
				for (DimensionedDataSource<?> source : nBundle.doubles) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.DBL, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : nBundle.floats) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.FLT, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : nBundle.int8s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.INT8, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : nBundle.int16s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.INT16, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : nBundle.int32s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.INT32, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : nBundle.int64s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.INT64, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : nBundle.uint1s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT1, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : nBundle.uint8s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT8, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : nBundle.uint16s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT16, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : nBundle.uint32s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT32, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : nBundle.uint64s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT64, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				System.out.println("netcdf files loaded");
			}
		});

		JButton loadScifio = new JButton("Load using scifio");
		loadScifio.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			@SuppressWarnings("unchecked")
			public void mouseClicked(MouseEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.showOpenDialog(frame);
				File f = chooser.getSelectedFile();

				nom.bdezonia.zorbage.scifio.DataBundle sBundle =
						Scifio.loadAllDatasets(f.getAbsolutePath());

				for (DimensionedDataSource<?> source : sBundle.uint1s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT1, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint2s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT2, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint3s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT3, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint4s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT4, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint5s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT5, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint6s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT6, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint7s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT7, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint8s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT8, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint9s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT9, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint10s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT10, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint11s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT11, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint12s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT12, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint13s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT13, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint14s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT14, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint15s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT15, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint16s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT16, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint32s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT32, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint64s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT64, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.uint128s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UINT128, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.int8s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.INT8, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.int16s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.INT16, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.int32s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.INT32, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.int64s) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.INT64, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.argbs) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.ARGB, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.bigs) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.UNBOUND, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.cdoubles) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.CDBL, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.cfloats) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.CFLT, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.doubles) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.DBL, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				for (DimensionedDataSource<?> source : sBundle.floats) {
					Tuple2<T,DimensionedDataSource<U>> tuple =
							new Tuple2<T, DimensionedDataSource<U>>((T) G.FLT, (DimensionedDataSource<U>) source);
					dataSources.add(tuple);
				}
				System.out.println("scifio files loaded");
			}
		});

		frame.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == KeyEvent.VK_UP) {
					if (imgNumber >= dataSources.size())
						java.awt.Toolkit.getDefaultToolkit().beep();
					else {
						imgNumber++;
						Tuple2<T, DimensionedDataSource<U>> tuple =
								dataSources.get(imgNumber);
						displayImage(tuple);
					}
				}
				if (e.getKeyChar() == KeyEvent.VK_DOWN) {
					if (imgNumber <= 0)
						java.awt.Toolkit.getDefaultToolkit().beep();
					else {
						imgNumber--;
						Tuple2<T, DimensionedDataSource<U>> tuple =
								dataSources.get(imgNumber);
						displayImage(tuple);
					}
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
			}
		});
		BorderLayout screenLayout = new BorderLayout();
		Container buttonContainer = new Container();
		buttonContainer.add(loadGdal);
		buttonContainer.add(loadNetcdf);
		buttonContainer.add(loadScifio);
		screenLayout.addLayoutComponent(buttonContainer, BorderLayout.NORTH);
		screenLayout.addLayoutComponent(
				new JLabel("Press arrow keys to switch datasets"),
				BorderLayout.SOUTH);
		
		frame.getContentPane().setLayout(screenLayout);
 
		frame.pack();
		
		frame.setVisible(true);
	}

	private	void displayImage(Tuple2<T, DimensionedDataSource<U>> tuple)
	{
		U min = tuple.a().construct();
		U max = tuple.a().construct();
		if (preferDataRange) {
			pixelDataBounds(tuple.a(), tuple.b().rawData(), min, max);
			if (tuple.a().isEqual().call(min, max))
				pixelTypeBounds(tuple.a(), min, max);
		}
		else {
			pixelTypeBounds(tuple.a(), min, max);
			if (tuple.a().isEqual().call(min, max))
				pixelDataBounds(tuple.a(), tuple.b().rawData(), min, max);
		}
		if (tuple.a().isEqual().call(min, max)) {
			if ((min instanceof RgbMember) || (min instanceof ArgbMember))
			{
				displayColorImage(tuple.a(), tuple.b());
				return;
			}
			else if ((min instanceof ComplexFloat32Member) ||
					(min instanceof ComplexFloat64Member))
			{
				displayComplexImage(tuple.a(), tuple.b());
				return;
			}
			// else min == max: a real image with zero display range
		}
		displayRealImage(tuple.a(), tuple.b(), min, max);
	}
	
	private void displayColorImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A COLOR IMAGE"+data.getName());
	}
	
	private void displayComplexImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A COMPLEX IMAGE "+data.getName());
	}

	// how would you display complex data? a channel for r and i? otherwise it's not
	//   bounded and it's not ordered
	
	private	void displayRealImage(T alg, DimensionedDataSource<U> data, U min, U max)
	{
		U value = alg.construct();
		boolean isHighPrec = value instanceof HighPrecRepresentation;
		if (data.numDimensions() < 1)
			throw new IllegalArgumentException("dataset is completely void: nothing to display");
		long width = data.dimension(0);
		long height = data.numDimensions() == 1 ? 1 : data.dimension(1);
		IntegerIndex idx = new IntegerIndex(data.numDimensions());
		long[] minPt = new long[data.numDimensions()];
		long[] maxPt = new long[data.numDimensions()];
		for (int i = 0; i < data.numDimensions(); i++) {
			maxPt[i] = data.dimension(i) - 1;
		}
		if (data.numDimensions() > 2) {
			// maybe we want to choose the right plane
			// for now additional coords are 0's so we are getting 1st plane
		}
		
		BufferedImage img = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_USHORT_GRAY);
		
		 // Safe cast as img is of type TYPE_USHORT_GRAY 
		
		DataBufferUShort buffer = (DataBufferUShort) img.getRaster().getDataBuffer();

		// Conveniently, the buffer already contains the data array
		short[] arrayUShort = buffer.getData();

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
			hpMin = new HighPrecisionMember(min.toString());
			hpMax = new HighPrecisionMember(max.toString());
		}

		SamplingIterator<IntegerIndex> iter = GridIterator.compute(minPt, maxPt);
		while (iter.hasNext()) {
			iter.next(idx);
			data.get(idx, value);

			// scale pixel to 0-65535
			if (isHighPrec) {
				((HighPrecRepresentation) value).toHighPrec(hpPix);
			}
			else {
				// expensive here
				String pxStrValue = value.toString();
				hpPix = new HighPrecisionMember(pxStrValue);
			}

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
			int pixelValue = BigDecimal.valueOf(65535.0).multiply(ratio).intValue();

			// TODO: should I use rgb buffer and scale to ints and let the colors
			//   just be whatever?
			
			// put in canvas
			long x = idx.get(0);
			long y = (data.numDimensions() == 1 ? 0 : idx.get(1));
			int pos = (int) (x + y * width);
			
			arrayUShort[pos] = (short) pixelValue;
		}
		
		((BorderLayout) frame.getContentPane().getLayout()).addLayoutComponent(
				new JLabel(new ImageIcon(img)), BorderLayout.CENTER);

		frame.pack();
		frame.repaint();
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
}
