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
 * Copyright (C) 2010-2011 - J.W. Janssen, http://www.lxtreme.nl
 */
package nl.lxtreme.ols.tool.tm1640;


import nl.lxtreme.ols.api.acquisition.*;
import nl.lxtreme.ols.api.data.*;

/**
 * 
 */
public final class TM1640DataSet extends BaseDataSet<TM1640Data>
{
  // CONSTANTS

  public static final String TM1640_START = "START";
  public static final String TM1640_STOP = "STOP";

  // VARIABLES

  private int decodedBytes;

  // CONSTRUCTORS

  /**
   * 
   */
  public TM1640DataSet( final int aStartSampleIdx, final int aStopSampleIdx, final AcquisitionResult aData )
  {
    super( aStartSampleIdx, aStopSampleIdx, aData );

    this.decodedBytes = 0;
  }

  // METHODS

  /**
   * Returns the number of decoded bytes.
   * 
   * @return the number of decoded bytes, >= 0.
   */
  public int getDecodedByteCount()
  {
    return this.decodedBytes;
  }

  /**
   * @param aTime
   * @param aByteValue
   */
  public void reportData( final int aChannelIdx, final int aStartSampleIdx, final int aEndSampleIdx,
      final int aByteValue )
  {
    final int idx = size();
    this.decodedBytes++;
    addData( new TM1640Data( idx, aChannelIdx, aStartSampleIdx, aEndSampleIdx, aByteValue ) );
  }
  
  /**
   * @param aTime
   */
  public void reportStartCondition( final int aChannelIdx, final int aSampleIdx )
  {
    final int idx = size();
    addData( new TM1640Data( idx, aChannelIdx, aSampleIdx, "START" ) );
  }

  /**
   * @param aTime
   */
  public void reportStopCondition( final int aChannelIdx, final int aSampleIdx )
  {
    final int idx = size();
    addData( new TM1640Data( idx, aChannelIdx, aSampleIdx, "STOP" ) );
  }

  public static String decodeSegments(int byteValue) {
	  switch (byteValue & 0x7F) {
	  case 0x7F: return "8"; // 0b01111111
	  case 0x06: return "I"; // 0b00000110
	  case 0x71: return "F"; // 0b01110001
	  case 0x08: return "_"; // 0b00001000
	  case 0x6D: return "S"; // 0b01101101
	  case 0x76: return "H"; // 0b01110110
	  case 0x3E: return "U"; // 0b00111110
	  case 0x38: return "L"; // 0b00111000
	  case 0x79: return "E"; // 0b01111001
	  case 0x39: return "C"; // 0b00111001
	  case 0x3F: return "O"; // 0b00111111
	  }
	return "";
  }

}

/* EOF */
