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
import java.awt.Component;
import java.awt.Container;
import java.awt.Panel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import nom.bdezonia.zorbage.misc.DataBundle;
import nom.bdezonia.zorbage.netcdf.NetCDF;
import nom.bdezonia.zorbage.sampling.IntegerIndex;
import nom.bdezonia.zorbage.sampling.SamplingIterator;
import nom.bdezonia.zorbage.scifio.Scifio;
import nom.bdezonia.zorbage.tuple.Tuple2;
import nom.bdezonia.zorbage.type.character.FixedStringMember;
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
	
	private int imgNumber = 0;

	private boolean preferDataRange = true;
	
	private Component image = null;
	
	private int[] colorTable = null;

	@SuppressWarnings({"rawtypes","unchecked"})
	public static void main(String[] args) {

		Gdal.init();

		Main main = new Main();
		
		main.colorTable = main.defaultColorTable();
		
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
			public void mouseClicked(MouseEvent e) {

				JFileChooser chooser = new JFileChooser();
				
				chooser.showOpenDialog(frame);
				
				File f = chooser.getSelectedFile();

				DataBundle bundle = Gdal.loadAllDatasets(f.getAbsolutePath());
	
				int nextImage = dataSources.size();
				
				dataSources.addAll(bundle.bundle());
				
				System.out.println("gdal files loaded");
				
				displayImage(dataSources.get(nextImage));
				
				imgNumber = nextImage;
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
			public void mouseClicked(MouseEvent e) {

				JFileChooser chooser = new JFileChooser();
				
				chooser.showOpenDialog(frame);
				
				File f = chooser.getSelectedFile();

				DataBundle bundle = NetCDF.loadAllDatasets(f.getAbsolutePath());
	
				int nextImage = dataSources.size();
				
				dataSources.addAll(bundle.bundle());
				
				System.out.println("netcdf files loaded");
				
				displayImage(dataSources.get(nextImage));
				
				imgNumber = nextImage;
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
			public void mouseClicked(MouseEvent e) {

				JFileChooser chooser = new JFileChooser();
				
				chooser.showOpenDialog(frame);
				
				File f = chooser.getSelectedFile();

				DataBundle bundle = Scifio.loadAllDatasets(f.getAbsolutePath());
	
				int nextImage = dataSources.size();
				
				dataSources.addAll(bundle.bundle());
				
				System.out.println("scifio files loaded");
								
				displayImage(dataSources.get(nextImage));
				
				imgNumber = nextImage;
			}
		});

		JButton displayNext = new JButton("Display Next");
		displayNext.addMouseListener(new MouseListener() {
			
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
			public void mouseClicked(MouseEvent e) {
				if (imgNumber+1 >= dataSources.size())
					java.awt.Toolkit.getDefaultToolkit().beep();
				else {
					imgNumber++;
					displayImage(dataSources.get(imgNumber));
				}
			}
		});

		JButton displayPrev = new JButton("Display Prev");
		displayPrev.addMouseListener(new MouseListener() {
			
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
			public void mouseClicked(MouseEvent e) {
				if (imgNumber-1 < 0)
					java.awt.Toolkit.getDefaultToolkit().beep();
				else {
					imgNumber--;
					displayImage(dataSources.get(imgNumber));
				}
			}
		});

		JButton loadLut = new JButton("Choose LUT");
		loadLut.addMouseListener(new MouseListener() {
			
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
			public void mouseClicked(MouseEvent e) {
				JFileChooser dlg = new JFileChooser();
				int returnVal = dlg.showDialog(frame, "OK");
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File lutFile = dlg.getSelectedFile();
					colorTable = loadLUT(lutFile);
					if (imgNumber >= 0 && imgNumber < dataSources.size()) {
						displayImage(dataSources.get(imgNumber));
					}
				}
			}
		});

		JButton resetLut = new JButton("Reset LUT");
		resetLut.addMouseListener(new MouseListener() {
			
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
			public void mouseClicked(MouseEvent e) {
				colorTable = defaultColorTable();
				if (imgNumber >= 0 && imgNumber < dataSources.size()) {
					displayImage(dataSources.get(imgNumber));
				}
			}
		});

		BufferedImage img = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		
		image = new JLabel(new ImageIcon(img));
				
		Panel bp = new Panel();
		
		bp.add(loadGdal);
		bp.add(loadNetcdf);
		bp.add(loadScifio);

		Panel ip = new Panel();
		
		ip.add(displayPrev);
		ip.add(displayNext);
		ip.add(loadLut);
		ip.add(resetLut);
		
		Container pane = frame.getContentPane();
		
		pane.setLayout(new BorderLayout());
		
		pane.add(bp, BorderLayout.NORTH);
		pane.add(ip, BorderLayout.SOUTH);
		pane.add(image, BorderLayout.CENTER);
		
		frame.pack();

		frame.setVisible(true);
	}

	private	void displayImage(Tuple2<T, DimensionedDataSource<U>> tuple)
	{
		// TODO: there now are all kinds of multidim data sources theoretically possible:
		//   (though no reader yet creates them). For instance:
		//     An n-d data structure containing vectors or matrices or tensors at each point.
		//     All of these can be made of reals, complexes, quaternions, or octonions. What
		//     would be the best way to display this info?
		
		U type = tuple.a().construct();
		
		if (type instanceof FixedStringMember) {
			displayTextData(tuple.a(), tuple.b());
		}
		else if ((type instanceof RgbMember) || (type instanceof ArgbMember))
		{
			displayColorImage(tuple.a(), tuple.b());
		}
		else if ((type instanceof ComplexFloat32Member) ||
				(type instanceof ComplexFloat64Member))
		{
			displayComplexImage(tuple.a(), tuple.b());
		}
		else {
			displayRealImage(tuple.a(), tuple.b());
		}
	}
	
	private void displayColorImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A COLOR IMAGE"+data.getName());
	}
	
	// how would you display complex data? a channel for r and i? otherwise it's not
	//   bounded and it's not ordered so you can't get a display range from it.
	
	private void displayComplexImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A COMPLEX IMAGE "+data.getName());
	}

	@SuppressWarnings("unchecked")
	private void displayTextData(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY TEXT DATA "+data.getName());
		FixedStringMember value = G.FSTRING.construct("something");
		data.rawData().get(0, (U) value);
		System.out.println(" contents = "+value.toString());
	}

	private	void displayRealImage(T alg, DimensionedDataSource<U> data)
	{
		U min = alg.construct();
		U max = alg.construct();
		if (preferDataRange) {
			pixelDataBounds(alg, data.rawData(), min, max);
			if (alg.isEqual().call(min, max))
				pixelTypeBounds(alg, min, max);
		}
		else {
			pixelTypeBounds(alg, min, max);
			if (alg.isEqual().call(min, max))
				pixelDataBounds(alg, data.rawData(), min, max);
		}
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
		
		BufferedImage img = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
		
		// Safe cast as img is of correct type 
		
		DataBufferInt buffer = (DataBufferInt) img.getRaster().getDataBuffer();

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

		SamplingIterator<IntegerIndex> iter = GridIterator.compute(minPt, maxPt);
		while (iter.hasNext()) {
			iter.next(idx);
			data.get(idx, value);

			if (isHighPrec) {
				((HighPrecRepresentation) value).toHighPrec(hpPix);
			}
			else {
				// expensive here
				String pxStrValue = value.toString();
				hpPix = new HighPrecisionMember(pxStrValue);
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
			
			// now cale 0-1 to the range of the size of the current color table
			
			int intensity = BigDecimal.valueOf(colorTable.length-1).multiply(ratio).intValue();

			// put a color from the colortable into the image at pos (x,y)
			
			long x = idx.get(0);
			long y = (data.numDimensions() == 1 ? 0 : idx.get(1));
			
			int bufferPos = (int) (y * width + x);
			
			arrayInt[bufferPos] = colorTable[intensity];
		}
		
		Container pane = frame.getContentPane();
		
		pane.remove(image);
		
		image = new JLabel(new ImageIcon(img));
		
		image = new JScrollPane(image);
		
		pane.add(image, BorderLayout.CENTER);

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

	private int[] defaultColorTable() {
		Integer[] colors = new Integer[256*256*256];
		// fill table
		int i = 0;
		for (int r = 0; r < 256; r++) {
			for (int g = 0; g < 256; g++) {
				for (int b = 0; b < 256; b++) {
					colors[i++] = argb(0x7f, r, g, b);
				}
			}
		}
		// sort it by intensity
		Arrays.sort(colors, new Comparator<Integer>() {

		@Override
		public int compare(Integer color1, Integer color2) {
			int r1 = r(color1);
			int g1 = g(color1);
			int b1 = b(color1);
			int r2 = r(color2);
			int g2 = g(color2);
			int b2 = b(color2);

			double bright1 = 0.2126 * r1 + 0.7152 * g1 + 0.0722 * b1;
			double bright2 = 0.2126 * r2 + 0.7152 * g2 + 0.0722 * b2;
			if (bright1 < bright2)
				return -1;
			else if (bright1 > bright2)
				return 1;
			else if (r1 < r2)
				return -1;
			else if (r1 > r2)
				return 1;
			else if (g1 < g2)
				return -1;
			else if (g1 > g2)
				return 1;
			else if (b1 < b2)
				return -1;
			else if (b1 > b2)
				return 1;
			else
				return 0;
			}
		});

		int[] primitives = new int[colors.length];
		for (i = 0; i < colors.length; i++)
			primitives[i] = colors[i];
		return primitives;
	}
	
	private int[] loadLUT(File lutFile) {
		/*
		 * This is a 32k LUT type defined in CUBE files. I found a few free LUTS.
		try {
			FileReader fr = new FileReader(lutFile);
			BufferedReader br = new BufferedReader(fr);
			for (int i = 0; i < 12; i++) {
				br.readLine();
			}
			int[] colors = new int[32768];
			for (int i = 0; i < colors.length; i++) {
				String line = br.readLine();
				String[] pieces = line.trim().split(" ");
				int r = (int) Math.round(255 * Double.valueOf(pieces[0]));
				int g = (int) Math.round(255 * Double.valueOf(pieces[1]));
				int b = (int) Math.round(255 * Double.valueOf(pieces[2]));
				colors[i] = argb(0x7f,r,g,b);
			}
			br.close();
			return colors;
		} catch (Exception e) {
			System.out.println("loadLUT exception "+e);
			return colorTable;
		}
		*/

		/*
		 * This is my best guess on how to load ImageJ LUT files. My code discovers that
		 * some of them are not stored in triplets so this code is not yet complete.
		 */
		FileInputStream fin = null;
		try {

            if (lutFile.length() > Integer.MAX_VALUE)
            	throw new IllegalArgumentException("lut data is too large");

            if (lutFile.length() % 3 != 0)
            	throw new IllegalArgumentException("lut data is not just r g b byte triples");

            byte fileContent[] = new byte[(int)lutFile.length()];
             
            fin = new FileInputStream(lutFile);
            
            // Reads up to certain bytes of data from this input stream into an array of bytes.
            fin.read(fileContent);
            
            fin.close();
            
            int chunk = fileContent.length / 3;
            
            int[] lut = new int[chunk];
            
            for (int i = 0; i < chunk; i++) {
            	lut[i] = argb(0xcf, fileContent[0*chunk + i], fileContent[1*chunk + i], fileContent[2*chunk + i]);
            }

            return lut;
            
		} catch (Exception e) {
			
			System.out.println("loadLUT exception "+e);
			
			return colorTable;
			
		} finally {
			try {
				if (fin != null)
					fin.close();
			} catch (Exception e) {
				;
			}
		}
	}

	private int argb(int a, int r, int g, int b) {
		return ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
	}

	@SuppressWarnings("unused")
	private int a(int color) {
		return (color >> 24) & 0xff;
	}
		    
	private int r(int color) {
		return (color >> 16) & 0xff;
	}
		    
	private int g(int color) {
		return (color >> 8) & 0xff;
	}
		    
	private int b(int color) {
		return (color >> 0) & 0xff;
	}
}
