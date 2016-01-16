/*
 * OpenBench LogicSniffer / SUMP project 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 * 
 * Copyright (C) 2010-2011 J.W. Janssen, www.lxtreme.nl
 */
package nl.lxtreme.ols.tool.tm1640;


import static org.junit.Assert.*;

import java.net.*;
import java.util.*;

import nl.lxtreme.ols.api.acquisition.*;
import nl.lxtreme.ols.api.data.annotation.AnnotationListener;
import nl.lxtreme.ols.api.tools.*;
import nl.lxtreme.ols.test.*;
import nl.lxtreme.ols.test.data.*;

import org.junit.*;
import org.mockito.*;


/**
 * Tests the decoding of datagrams works correctly.
 */
public class TM1640AnalyserWorkerContentTest
{
  // METHODS

  /**
   * @param aDataSet
   * @param aEventName
   * @return
   */
  private static void assertDataEvents( final TM1640DataSet aDataSet, final int... aExpectedData )
  {
    final Iterator<TM1640Data> dataIter = aDataSet.getData().iterator();

    int i = 0;
    while ( dataIter.hasNext() && ( i < aExpectedData.length ) )
    {
      final TM1640Data data = dataIter.next();
      if ( !data.isEvent() )
      {
        assertEquals( "Data value at index " + i + " not equal, ", aExpectedData[i], data.getValue() );
        i++;
      }
    }
    assertEquals( "Not all data events were seen?!", aExpectedData.length, i );
  }

  /**
   * Test method for
   * {@link nl.lxtreme.ols.tool.tm1640.TM1640AnalyserTask#doInBackground()}.
   */
  @Test
  public void testKopterDecodingOk() throws Exception
  {
    final TM1640DataSet result = analyseDataFile( "tm1640_3.ols", 0, 1 );
    assertNotNull( result );

    assertDataEvents( result, 0x52, 0x11, 0x54, 0x10, 0x56, 0x12, 0x58, 0x0B, 0x57, 0x11, 0x52, 0x13, 0x54, 0x11 );
  }

  /**
   * Analyzes the data file identified by the given resource name.
   * 
   * @param aResourceName
   *          the name of the resource (= data file) to analyse, cannot be
   *          <code>null</code>.
   * @return the analysis results, never <code>null</code>.
   * @throws Exception
   *           in case of exceptions.
   */
  private TM1640DataSet analyseDataFile( final String aResourceName, final int clkIndex, final int dinIndex )
      throws Exception
  {
    URL resource = ResourceUtils.getResource( getClass(), aResourceName );
    AcquisitionResult container = DataTestUtils.getCapturedData( resource );
    ToolContext toolContext = DataTestUtils.createToolContext( container );

    ToolProgressListener progressListener = Mockito.mock( ToolProgressListener.class );
    AnnotationListener annotationListener = Mockito.mock( AnnotationListener.class );

    TM1640AnalyserTask worker = new TM1640AnalyserTask( toolContext, progressListener, annotationListener );
    worker.setClkIndex( clkIndex );
    worker.setDinIndex( dinIndex );

    // Simulate we're running in a separate thread by directly calling the main
    // working routine...
    TM1640DataSet result = worker.call();
    assertNotNull( result );

    return result;
  }
}
