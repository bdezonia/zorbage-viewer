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

import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algebra.G;
import nom.bdezonia.zorbage.algorithm.PolarCoords;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.data.DimensionedStorage;
import nom.bdezonia.zorbage.misc.DataSourceUtils;
import nom.bdezonia.zorbage.sampling.IntegerIndex;
import nom.bdezonia.zorbage.sampling.SamplingCartesianIntegerGrid;
import nom.bdezonia.zorbage.sampling.SamplingIterator;
import nom.bdezonia.zorbage.type.complex.float64.ComplexFloat64Member;
import nom.bdezonia.zorbage.type.real.float64.Float64Algebra;
import nom.bdezonia.zorbage.type.real.float64.Float64Member;

/**
 * 
 * @author Barry DeZonia
 *
 */
public class ComplexImageViewer<L extends Algebra<L,M>, M, N extends Algebra<N,O>, O> {

	@SuppressWarnings("unchecked")
	public ComplexImageViewer(L complexAlgebra, N realAlgebra, DimensionedDataSource<M> data) {

		long[] origDims = DataSourceUtils.dimensions(data);
		
		int numD = origDims.length;
		
		DimensionedDataSource<Float64Member> complexMagnitudes = DimensionedStorage.allocate(G.DBL.construct(), origDims);
		
		IntegerIndex min = new IntegerIndex(numD);

		IntegerIndex max = new IntegerIndex(numD);
		
		for (int i = 0; i < numD; i++) {
			
			max.set(i, data.dimension(i) - 1);
		}
		
		SamplingCartesianIntegerGrid sampling = new SamplingCartesianIntegerGrid(min, max);
		
		SamplingIterator<IntegerIndex> iter = sampling.iterator();
		
		IntegerIndex location = new IntegerIndex(numD);

		ComplexFloat64Member complexValue = G.CDBL.construct();

		Float64Member real = G.DBL.construct();
		
		Float64Member imag = G.DBL.construct();
		
		Float64Member magnitude = G.DBL.construct();
		
		while (iter.hasNext()) {
			
			iter.next(location);

			// TODO: FIXME later. Provide a more general approach than assuming complex doubles.
			
			((DimensionedDataSource<ComplexFloat64Member>) data).get(location, complexValue);
			
			complexValue.getR(real);
			
			complexValue.getI(imag);
			
			PolarCoords.magnitude(G.DBL, real, imag, magnitude);
			
			complexMagnitudes.set(location, magnitude);
		}
		
		complexMagnitudes.setName("Complex magnitudes of "+data.getName());

		complexMagnitudes.setSource(data.getSource());
		
		new RealImageViewer<Float64Algebra,Float64Member>(G.DBL, complexMagnitudes);
	}
}
