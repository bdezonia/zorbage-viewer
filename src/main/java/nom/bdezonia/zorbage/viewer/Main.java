package nom.bdezonia.zorbage.viewer;

import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.io.File;
import java.math.BigDecimal;

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
import nom.bdezonia.zorbage.type.highprec.real.HighPrecisionAlgebra;
import nom.bdezonia.zorbage.type.highprec.real.HighPrecisionMember;

public class Main {

	@SuppressWarnings("unused")
	private static nom.bdezonia.zorbage.gdal.DataBundle gBundle = null;
	
	@SuppressWarnings("unused")
	private static nom.bdezonia.zorbage.netcdf.DataBundle nBundle = null;
	
	@SuppressWarnings("unused")
	private static nom.bdezonia.zorbage.scifio.DataBundle sBundle = null;

	private static JFrame frame = null;
	
    public static void main(String[] args) {

    	Gdal.init();
		
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {

    	//Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);
 
        //Create and set up the window.
        frame = new JFrame("Zorbage Data Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        JButton loadGdal   = new JButton("Load using gdal");
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
				gBundle = Gdal.loadAllDatasets(f.getAbsolutePath());
				if (gBundle.uint8s.size() > 0)
					displayImage(G.UINT8, true, gBundle.uint8s.get(0));
				else if (gBundle.uint16s.size() > 0)
					displayImage(G.UINT16, true, gBundle.uint16s.get(0));
				else if (gBundle.uint8s.size() > 0)
					displayImage(G.UINT32, true, gBundle.uint32s.get(0));
				else if (gBundle.int16s.size() > 0)
					displayImage(G.INT16, true, gBundle.int16s.get(0));
				else if (gBundle.int32s.size() > 0)
					displayImage(G.INT32, true, gBundle.int32s.get(0));
				if (gBundle.doubles.size() > 0)
					displayImage(G.DBL, true, gBundle.doubles.get(0));
				else if (gBundle.floats.size() > 0)
					displayImage(G.FLT, true, gBundle.floats.get(0));
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
			public void mouseClicked(MouseEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.showOpenDialog(frame);
				File f = chooser.getSelectedFile();
				nBundle = NetCDF.loadAllDatasets(f.getAbsolutePath());
				if (nBundle.uint1s.size() > 0)
					displayImage(G.UINT1, true, nBundle.uint1s.get(0));
				else if (nBundle.uint8s.size() > 0)
					displayImage(G.UINT8, true, nBundle.uint8s.get(0));
				else if (nBundle.uint16s.size() > 0)
					displayImage(G.UINT16, true, nBundle.uint16s.get(0));
				else if (nBundle.uint32s.size() > 0)
					displayImage(G.UINT32, true, nBundle.uint32s.get(0));
				else if (nBundle.uint64s.size() > 0)
					displayImage(G.UINT64, true, nBundle.uint64s.get(0));
				else if (nBundle.int8s.size() > 0)
					displayImage(G.INT8, true, nBundle.int8s.get(0));
				else if (nBundle.int16s.size() > 0)
					displayImage(G.INT16, true, nBundle.int16s.get(0));
				else if (nBundle.int32s.size() > 0)
					displayImage(G.INT32, true, nBundle.int32s.get(0));
				else if (nBundle.int64s.size() > 0)
					displayImage(G.INT64, true, nBundle.int64s.get(0));
				else if (nBundle.doubles.size() > 0)
					displayImage(G.DBL, true, nBundle.doubles.get(0));
				else if (nBundle.floats.size() > 0)
					displayImage(G.FLT, true, nBundle.floats.get(0));
				else
					System.out.println("skipping something? nothing loaded.");
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
			public void mouseClicked(MouseEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.showOpenDialog(frame);
				File f = chooser.getSelectedFile();
				sBundle = Scifio.loadAllDatasets(f.getAbsolutePath());
				if (sBundle.uint1s.size() > 0)
					displayImage(G.UINT1, true, sBundle.uint1s.get(0));
				else if (sBundle.uint2s.size() > 0)
					displayImage(G.UINT2, true, sBundle.uint2s.get(0));
				else if (sBundle.uint3s.size() > 0)
					displayImage(G.UINT3, true, sBundle.uint3s.get(0));
				else if (sBundle.uint4s.size() > 0)
					displayImage(G.UINT4, true, sBundle.uint4s.get(0));
				else if (sBundle.uint5s.size() > 0)
					displayImage(G.UINT5, true, sBundle.uint5s.get(0));
				else if (sBundle.uint6s.size() > 0)
					displayImage(G.UINT6, true, sBundle.uint6s.get(0));
				else if (sBundle.uint7s.size() > 0)
					displayImage(G.UINT7, true, sBundle.uint7s.get(0));
				else if (sBundle.uint8s.size() > 0)
					displayImage(G.UINT8, true, sBundle.uint8s.get(0));
				else if (sBundle.uint9s.size() > 0)
					displayImage(G.UINT9, true, sBundle.uint9s.get(0));
				else if (sBundle.uint10s.size() > 0)
					displayImage(G.UINT10, true, sBundle.uint10s.get(0));
				else if (sBundle.uint11s.size() > 0)
					displayImage(G.UINT11, true, sBundle.uint11s.get(0));
				else if (sBundle.uint12s.size() > 0)
					displayImage(G.UINT12, true, sBundle.uint12s.get(0));
				else if (sBundle.uint13s.size() > 0)
					displayImage(G.UINT13, true, sBundle.uint13s.get(0));
				else if (sBundle.uint14s.size() > 0)
					displayImage(G.UINT14, true, sBundle.uint14s.get(0));
				else if (sBundle.uint15s.size() > 0)
					displayImage(G.UINT15, true, sBundle.uint15s.get(0));
				else if (sBundle.uint16s.size() > 0)
					displayImage(G.UINT16, true, sBundle.uint16s.get(0));
				else if (sBundle.uint32s.size() > 0)
					displayImage(G.UINT32, true, sBundle.uint32s.get(0));
				else if (sBundle.uint64s.size() > 0)
					displayImage(G.UINT64, true, sBundle.uint64s.get(0));
				else if (sBundle.uint128s.size() > 0)
					displayImage(G.UINT128, true, sBundle.uint128s.get(0));
				else if (sBundle.int8s.size() > 0)
					displayImage(G.INT8, true, sBundle.int8s.get(0));
				else if (sBundle.int16s.size() > 0)
					displayImage(G.INT16, true, sBundle.int16s.get(0));
				else if (sBundle.int32s.size() > 0)
					displayImage(G.INT32, true, sBundle.int32s.get(0));
				else if (sBundle.int64s.size() > 0)
					displayImage(G.INT64, true, sBundle.int64s.get(0));
				else if (sBundle.doubles.size() > 0)
					displayImage(G.DBL, true, sBundle.doubles.get(0));
				else if (sBundle.floats.size() > 0)
					displayImage(G.FLT, true, sBundle.floats.get(0));
				else if (sBundle.bigs.size() > 0)
					displayImage(G.UNBOUND, true, sBundle.bigs.get(0));
				else
					System.out.println("skipping something: Arbgs, cmplxfloats, cmplxdoubles");
				System.out.println("scifio files loaded");
			}
		});

        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(loadGdal);
        frame.getContentPane().add(loadNetcdf);
        frame.getContentPane().add(loadScifio);
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    // how would you display complex data? a channel for r and i? otherwise it's not
    //   bounded and it's not ordered
    
    private static <T extends Algebra<T,U>, U>
    void displayImage(T alg, boolean preferDataBounds, DimensionedDataSource<U> data) {
    	U min = alg.construct();
    	U max = alg.construct();
    	if (preferDataBounds) {
    		pixelDataBound(alg, data.rawData(), min, max);
        	if (alg.isEqual().call(min, max)) {
            	pixelTypeBounds(alg, min, max);
        	}
    	}
    	if (!preferDataBounds) {
        	pixelTypeBounds(alg, min, max);
        	if (alg.isEqual().call(min, max)) {
        		pixelDataBound(alg, data.rawData(), min, max);
        	}
    	}
    	if (alg.isEqual().call(min, max)) {
    		// kinda pooched here. throw exception? or fake the range?
    		alg.assign().call(alg.construct("0"), min);
    		alg.assign().call(alg.construct("255"), max);
    	}
    	U value = alg.construct();
    	boolean useCast = value instanceof HighPrecRepresentation;
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
    	
    	DataBufferUShort buffer = (DataBufferUShort) img.getRaster().getDataBuffer(); // Safe cast as img is of type TYPE_USHORT_GRAY 

		// Conveniently, the buffer already contains the data array
		short[] arrayUShort = buffer.getData();

		HighPrecisionMember hpPix = new HighPrecisionMember();
		HighPrecisionMember hpMin;
		HighPrecisionMember hpMax;
		
		if (useCast) {
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
    		if (useCast) {
    			((HighPrecRepresentation) value).toHighPrec(hpPix);
    		}
    		else {
    			// expensive here
    			String pxStrValue = value.toString();
        		hpPix = new HighPrecisionMember(pxStrValue);
    		}

    		BigDecimal numer = hpPix.v().subtract(hpMin.v());
    		BigDecimal denom = hpMax.v().subtract(hpMin.v());
    		BigDecimal ratio = numer.divide(denom, HighPrecisionAlgebra.getContext());
    		if (ratio.compareTo(BigDecimal.ZERO) < 0)
    			ratio = BigDecimal.ZERO;
    		if (ratio.compareTo(BigDecimal.ONE) > 0)
    			ratio = BigDecimal.ONE;
    		int pixelValue = BigDecimal.valueOf(65535.0).multiply(ratio).intValue();
    		
    		// put in canvas
    		long x = idx.get(0);
    		long y = (data.numDimensions() == 1 ? 0 : idx.get(1));
    		int pos = (int) (x + y * width);
    		
    		arrayUShort[pos] = (short) pixelValue;
    	}
    	frame.getContentPane().add(new JLabel(new ImageIcon(img)));
    	frame.pack();
		frame.repaint();
    }
    
    private static <T extends Algebra<T,U>, U, V extends Algebra<V,U> & Bounded<U>>
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
    
    private static <T extends Algebra<T,U>, U, V extends Algebra<V,U> & Ordered<U>>
    void pixelDataBound(T alg, IndexedDataSource<U> data, U min, U max)
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
