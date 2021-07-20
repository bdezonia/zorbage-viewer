package nom.bdezonia.zorbage.viewer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.math.BigDecimal;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algebra.Bounded;
import nom.bdezonia.zorbage.algebra.HighPrecRepresentation;
import nom.bdezonia.zorbage.algebra.Ordered;
import nom.bdezonia.zorbage.algorithm.MinMaxElement;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.datasource.IndexedDataSource;
import nom.bdezonia.zorbage.dataview.PlaneView;
import nom.bdezonia.zorbage.misc.BigDecimalUtils;
import nom.bdezonia.zorbage.type.color.RgbUtils;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionAlgebra;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionMember;

public class RealImageViewer<T extends Algebra<T,U>, U> {

	private int[] colorTable = defaultColorTable();
	
	public RealImageViewer(T alg, DimensionedDataSource<U> dataSource) {

		System.out.println("numD = " + dataSource.numDimensions());
		
		boolean preferDataRange = true;
		
		int extraDimCount = dataSource.numDimensions() - 2;
		if (extraDimCount < 0) extraDimCount = 0;
		long[] extraDimPositions = new long[extraDimCount];
		PlaneView<U> view = new PlaneView<>(dataSource, 0, 1, extraDimPositions);
		
		String name = dataSource.getName();
		if (name == null)
			name = "<unknown name>";
		
		String source = dataSource.getSource();
		if (source == null)
			source = "<unknown source>";
		
		JFrame frame = new JFrame(name + " " + source);
		
		frame.setLayout(new BorderLayout());

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
		long width  = view.d0();
		long height = view.d1();
		
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

		for (int y = 0; y < view.d1(); y++) {
			for (int x = 0; x < view.d0(); x++) {

				view.get(x, y, value);
				
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
				
				int intensity =
						BigDecimal.valueOf(colorTable.length-1).multiply(ratio).add(BigDecimalUtils.ONE_HALF).intValue();

				// put a color from the color table into the image at pos (x,y)
				
				int bufferPos = (int) (y * width + x);
				
				arrayInt[bufferPos] = colorTable[intensity];
			}
		}
		
		JPanel graphicsPanel = new JPanel();
		JLabel image = new JLabel(new ImageIcon(img));
		JScrollPane scrollPane = new JScrollPane(image);
		graphicsPanel.add(scrollPane, BorderLayout.CENTER);
		
		JPanel positions = new JPanel();
		BoxLayout boxLayout = new BoxLayout(positions, BoxLayout.Y_AXIS);
		positions.setLayout(boxLayout);
		
		for (int i = 0; i < extraDimCount; i++) {
			JPanel miniPanel = new JPanel();
			miniPanel.setLayout(new FlowLayout());
			JButton decrementButton = new JButton("Decrement");
			JButton incrementButton = new JButton("Increment");
			JFormattedTextField longField = new JFormattedTextField();
			longField.setValue(0L);
			miniPanel.add(decrementButton);
			miniPanel.add(longField);
			miniPanel.add(incrementButton);
			positions.add(miniPanel);
		}
		
		JTextField readout = new JTextField();
		readout.setEnabled(false);
		
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BorderLayout());
		controlPanel.add(readout, BorderLayout.NORTH);
		controlPanel.add(positions, BorderLayout.CENTER);
		
		frame.add(graphicsPanel, BorderLayout.CENTER);
		frame.add(controlPanel, BorderLayout.SOUTH);
		
		frame.pack();

		frame.setVisible(true);

		frame.repaint();
		
		// make detached window
		// add bufferedImage in the middle
		// add coord readout at bottom
		// add extraDimCount sliders below that
		// add name and source of data source in the title bar
	}
	
	public void changeColorTable(int[] colorTable) {
		this.colorTable = colorTable;
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
			colors[i] = RgbUtils.argb(0xcf, r, g, b);
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
}
