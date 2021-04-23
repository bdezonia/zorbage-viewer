/*
 * zorbage-viewer: utility app for loading and viewing various image data formats
 *
 * Copyright (C) 2020-2021 Barry DeZonia
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algebra.Bounded;
import nom.bdezonia.zorbage.algebra.HighPrecRepresentation;
import nom.bdezonia.zorbage.algebra.Ordered;
import nom.bdezonia.zorbage.algorithm.GridIterator;
import nom.bdezonia.zorbage.algorithm.MinMaxElement;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.datasource.IndexedDataSource;
import nom.bdezonia.zorbage.gdal.Gdal;
import nom.bdezonia.zorbage.misc.DataBundle;
import nom.bdezonia.zorbage.netcdf.NetCDF;
import nom.bdezonia.zorbage.nifti.Nifti;
import nom.bdezonia.zorbage.sampling.IntegerIndex;
import nom.bdezonia.zorbage.sampling.SamplingIterator;
import nom.bdezonia.zorbage.scifio.Scifio;
import nom.bdezonia.zorbage.tuple.Tuple2;
import nom.bdezonia.zorbage.type.character.CharMember;
import nom.bdezonia.zorbage.type.color.ArgbMember;
import nom.bdezonia.zorbage.type.color.RgbMember;
import nom.bdezonia.zorbage.type.complex.float16.ComplexFloat16Member;
import nom.bdezonia.zorbage.type.complex.float32.ComplexFloat32Member;
import nom.bdezonia.zorbage.type.complex.float64.ComplexFloat64Member;
import nom.bdezonia.zorbage.type.complex.highprec.ComplexHighPrecisionMember;
import nom.bdezonia.zorbage.type.gaussian.int16.GaussianInt16Member;
import nom.bdezonia.zorbage.type.gaussian.int32.GaussianInt32Member;
import nom.bdezonia.zorbage.type.gaussian.int64.GaussianInt64Member;
import nom.bdezonia.zorbage.type.gaussian.int8.GaussianInt8Member;
import nom.bdezonia.zorbage.type.gaussian.unbounded.GaussianIntUnboundedMember;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionAlgebra;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionMember;
import nom.bdezonia.zorbage.type.string.FixedStringMember;
import nom.bdezonia.zorbage.type.string.StringMember;

/**
 * 
 * @author Barry DeZonia
 *
 */
public class Main<T extends Algebra<T,U>, U> {

	private JFrame frame = null;
	
	private List<Tuple2<T,DimensionedDataSource<U>>> dataSources = new ArrayList<>();
	
	private int dsNumber = 0;

	private boolean preferDataRange = true;
	
	private Component image = null;
	
	private int[] defaultColorTable = defaultColorTable();

	private int[] colorTable = defaultColorTable;

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {

		Gdal.init();

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
			public void mouseClicked(MouseEvent e) {

				JFileChooser chooser = new JFileChooser();
				
				chooser.showOpenDialog(frame);
				
				File f = chooser.getSelectedFile();

				DataBundle bundle = Gdal.loadAllDatasets(f.getAbsolutePath());
	
				int nextDs = dataSources.size();
				
				dataSources.addAll(bundle.bundle());
				
				displayData(dataSources.get(nextDs));
				
				dsNumber = nextDs;
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
	
				int nextDs = dataSources.size();
				
				dataSources.addAll(bundle.bundle());
				
				displayData(dataSources.get(nextDs));
				
				dsNumber = nextDs;
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
	
				int nextDs = dataSources.size();
				
				dataSources.addAll(bundle.bundle());
				
				displayData(dataSources.get(nextDs));
				
				dsNumber = nextDs;
			}
		});

		JButton loadNifti = new JButton("Load using nifti");
		loadNifti.addMouseListener(new MouseListener() {
			
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

				DataBundle bundle = Nifti.open(f.getAbsolutePath());
	
				int nextDs = dataSources.size();
				
				dataSources.addAll(bundle.bundle());
				
				displayData(dataSources.get(nextDs));
				
				dsNumber = nextDs;
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
				if (dsNumber+1 >= dataSources.size())
					java.awt.Toolkit.getDefaultToolkit().beep();
				else {
					dsNumber++;
					displayData(dataSources.get(dsNumber));
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
				if (dsNumber-1 < 0)
					java.awt.Toolkit.getDefaultToolkit().beep();
				else {
					dsNumber--;
					displayData(dataSources.get(dsNumber));
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
					if (dsNumber >= 0 && dsNumber < dataSources.size()) {
						displayData(dataSources.get(dsNumber));
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
				colorTable = defaultColorTable;
				if (dsNumber >= 0 && dsNumber < dataSources.size()) {
					displayData(dataSources.get(dsNumber));
				}
			}
		});

		BufferedImage img = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		
		image = new JLabel(new ImageIcon(img));
				
		Panel bp = new Panel();
		
		bp.add(loadGdal);
		bp.add(loadNetcdf);
		bp.add(loadNifti);
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

	private	void displayData(Tuple2<T, DimensionedDataSource<U>> tuple)
	{
		// TODO: there now are all kinds of multidim data sources theoretically possible:
		//   (though no reader yet creates them). For instance:
		//     An n-d data structure containing vectors or matrices or tensors at each point.
		//     All of these can be made of reals, complexes, quaternions, or octonions. What
		//     would be the best way to display this info?
		
		U type = tuple.a().construct();
		
		if ((type instanceof FixedStringMember) || (type instanceof StringMember) || (type instanceof CharMember)) {
			displayTextData(tuple.a(), tuple.b());
		}
		else if ((type instanceof RgbMember) || (type instanceof ArgbMember))
		{
			displayColorImage(tuple.a(), tuple.b());
		}
		else if ((type instanceof ComplexFloat16Member) ||
				(type instanceof ComplexFloat32Member) ||
				(type instanceof ComplexFloat64Member) ||
				(type instanceof ComplexHighPrecisionMember) ||
				(type instanceof GaussianInt8Member) ||
				(type instanceof GaussianInt16Member) ||
				(type instanceof GaussianInt32Member) ||
				(type instanceof GaussianInt64Member) ||
				(type instanceof GaussianIntUnboundedMember))
		{
			displayComplexImage(tuple.a(), tuple.b());
		}
		else {
			displayRealImage(tuple.a(), tuple.b());
		}
	}
	
	private void displayColorImage(T alg, DimensionedDataSource<U> data) {
		U value = alg.construct();
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

		SamplingIterator<IntegerIndex> iter = GridIterator.compute(minPt, maxPt);
		while (iter.hasNext()) {
			iter.next(idx);
			data.get(idx, value);
			int v;
			if (value instanceof ArgbMember) {
				ArgbMember tmp = (ArgbMember) value;
				v = (tmp.a() << 24) | (tmp.r() << 16) | (tmp.g() << 8) | (tmp.b() << 0);
			}
			else if (value instanceof RgbMember) {
				RgbMember tmp = (RgbMember) value;
				v = (255 << 24) | (tmp.r() << 16) | (tmp.g() << 8) | (tmp.b() << 0);
			}
			else
				throw new IllegalArgumentException("Unsupported color type passed to display routine");

			long x = idx.get(0);
			long y = (idx.numDimensions() == 1 ? 0 : idx.get(1));
			
			int bufferPos = (int) (y * width + x);
			
			arrayInt[bufferPos] = v;
		}
		
		Container pane = frame.getContentPane();
		
		pane.remove(image);
		
		image = new JLabel(new ImageIcon(img));
		
		image = new JScrollPane(image);
		
		pane.add(image, BorderLayout.CENTER);

		frame.setTitle("Zorbage Data Viewer - " + data.getName());
		
		frame.pack();

		frame.repaint();
	}
	
	// how would you display complex data? a channel for r and i? otherwise it's not
	//   bounded and it's not ordered so you can't get a display range from it. Also
	//   similar things for quat and oct images
	
	private void displayComplexImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A COMPLEX IMAGE "+data.getName());
	}

	@SuppressWarnings("unchecked")
	private void displayTextData(T alg, DimensionedDataSource<U> data) {

		Container pane = frame.getContentPane();
		
		pane.remove(image);
		
		image = new JTextPane();

		U type = alg.construct();
		
		String result = "";

		if (type instanceof StringMember) {
			StringMember value = new StringMember();
			IndexedDataSource<StringMember> stringList = (IndexedDataSource<StringMember>) data.rawData();
			for (long i = 0; i < stringList.size(); i++) {
				stringList.get(0, value);
				result = result + "\n" + value.toString();
			}
		}
		else if (type instanceof FixedStringMember) {
			FixedStringMember value = new FixedStringMember(256);
			IndexedDataSource<FixedStringMember> stringList = (IndexedDataSource<FixedStringMember>) data.rawData();
			for (long i = 0; i < stringList.size(); i++) {
				stringList.get(0, value);
				result = result + "\n" + value.toString();
			}
		}
		else if (type instanceof CharMember) {
			CharMember value = new CharMember();
			IndexedDataSource<CharMember> stringList = (IndexedDataSource<CharMember>) data.rawData();
			for (long i = 0; i < stringList.size(); i++) {
				stringList.get(0, value);
				result = result + value.toString();
			}
		}
		else
			throw new IllegalArgumentException("Unknown string type "+type);
		
		((JTextPane)image).setText(result);
		
		image = new JScrollPane(image);
		
		pane.add(image, BorderLayout.CENTER);

		frame.setTitle("Zorbage Data Viewer - " + data.getName());
		
		frame.pack();

		frame.repaint();
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
			
			// now scale 0-1 to the range of the size of the current color table
			
			int intensity = BigDecimal.valueOf(colorTable.length-1).multiply(ratio).intValue();

			// put a color from the color table into the image at pos (x,y)
			
			long x = idx.get(0);
			long y = (idx.numDimensions() == 1 ? 0 : idx.get(1));
			
			int bufferPos = (int) (y * width + x);
			
			arrayInt[bufferPos] = colorTable[intensity];
		}
		
		Container pane = frame.getContentPane();
		
		pane.remove(image);
		
		image = new JLabel(new ImageIcon(img));
		
		image = new JScrollPane(image);
		
		pane.add(image, BorderLayout.CENTER);

		frame.setTitle("Zorbage Data Viewer - " + data.getName());
		
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

		int[] colors = new int[256*3];
		int r = 0, g = 0, b = 0;
		for (int i = 0; i < colors.length; i++) {
			colors[i] = argb(0xcf, r, g, b);
			if (i % 3 == 0) {
				b++;
			}
			else if (i % 3 == 1) {
				r++;
			}
			else {
				g++;
			}
		}
		return colors;
	}
	
	private int[] loadLUT(File lutFile) {
		/*
		 * This is my best guess on how to load ImageJ LUT files. My code discovers that
		 * some of them are not stored in triplets so this code is not yet complete.
		 */
		FileInputStream fin = null;
		try {

			if (lutFile.length() > Integer.MAX_VALUE)
				throw new IllegalArgumentException("lut data is too large");

			byte fileContent[] = new byte[(int)lutFile.length()];

			fin = new FileInputStream(lutFile);

			// Reads up to certain bytes of data from this input stream into an array of bytes.
			fin.read(fileContent);

			fin.close();

			// note: some imagej lut files have sizes that are not divisible by 3. this code ignores the couple extra bytes.
			int chunk = fileContent.length / 3;

			int[] lut = new int[chunk];

			for (int i = 0; i < chunk; i++) {
				// TODO: why 0xcf? Why not 0xff? Does it make a difference?
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

	@SuppressWarnings("unused")
	private int r(int color) {
		return (color >> 16) & 0xff;
	}

	@SuppressWarnings("unused")
	private int g(int color) {
		return (color >> 8) & 0xff;
	}

	@SuppressWarnings("unused")
	private int b(int color) {
		return (color >> 0) & 0xff;
	}
}
