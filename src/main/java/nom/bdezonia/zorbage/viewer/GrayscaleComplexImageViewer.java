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

import nom.bdezonia.zorbage.algebra.AbsoluteValue;
import nom.bdezonia.zorbage.algebra.Addition;
import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algebra.Allocatable;
import nom.bdezonia.zorbage.algebra.GetI;
import nom.bdezonia.zorbage.algebra.GetR;
import nom.bdezonia.zorbage.algebra.Invertible;
import nom.bdezonia.zorbage.algebra.Multiplication;
import nom.bdezonia.zorbage.algebra.Ordered;
import nom.bdezonia.zorbage.algebra.Roots;
import nom.bdezonia.zorbage.algorithm.PolarCoords;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.data.DimensionedStorage;
import nom.bdezonia.zorbage.misc.DataSourceUtils;
import nom.bdezonia.zorbage.sampling.IntegerIndex;
import nom.bdezonia.zorbage.sampling.SamplingCartesianIntegerGrid;
import nom.bdezonia.zorbage.sampling.SamplingIterator;

/**
 * 
 * @author Barry DeZonia
 *
 */
public class GrayscaleComplexImageViewer
{
	// do not instantiate
	
	private GrayscaleComplexImageViewer() { }
	
	/**
	 * 
	 * @param <CA>
	 * @param <C>
	 * @param <RA>
	 * @param <R>
	 * @param cAlg
	 * @param rAlg
	 * @param input
	 * @return
	 */
	public static <CA extends Algebra<CA,C>,
					C extends GetR<R> & GetI<R>,
					RA extends Algebra<RA,R> & Roots<R> & Addition<R> &
								Multiplication<R> & Ordered<R> &
								AbsoluteValue<R,R> & Invertible<R>,
					R extends Allocatable<R>>
	
		RealImageViewer<RA,R>
	
			view(Algebra<?,?> cAlg, Algebra<?,?> rAlg, DimensionedDataSource<?> input)
	{
		@SuppressWarnings("unchecked")
		DimensionedDataSource<C> data = (DimensionedDataSource<C>) input;
		
		@SuppressWarnings("unchecked")
		CA complexAlgebra = (CA) cAlg;
		
		@SuppressWarnings("unchecked")
		RA realAlgebra = (RA) rAlg;
		
		long[] origDims = DataSourceUtils.dimensions(data);
		
		DimensionedDataSource<R> complexMagnitudes =
				
				DimensionedStorage.allocate(realAlgebra.construct(), origDims);
		
		SamplingCartesianIntegerGrid sampling =
				
				new SamplingCartesianIntegerGrid(origDims);
		
		SamplingIterator<IntegerIndex> iter = sampling.iterator();
		
		IntegerIndex location = new IntegerIndex(origDims.length);

		C complexValue = complexAlgebra.construct();

		R real = realAlgebra.construct();
		
		R imag = realAlgebra.construct();
		
		R magnitude = realAlgebra.construct();
		
		while (iter.hasNext()) {
			
			iter.next(location);

			data.get(location, complexValue);
			
			complexValue.getR(real);
			
			complexValue.getI(imag);
			
			PolarCoords.magnitude(realAlgebra, real, imag, magnitude);
			
			complexMagnitudes.set(location, magnitude);
		}
		
		complexMagnitudes.setName("Complex magnitudes of "+data.getName());

		complexMagnitudes.setSource(data.getSource());
		
		for (int i = 0; i < complexMagnitudes.numDimensions(); i++) {

			String type = input.getAxisType(i);
			
			String unit = input.getAxisUnit(i);
			
			complexMagnitudes.setAxisType(i, type);
			
			complexMagnitudes.setAxisUnit(i, unit);
		}
		
		return new RealImageViewer<>(realAlgebra, complexMagnitudes);
	}
}
