/*
 *  Copyright (c) 2012-2013 Thomas Dunnick (https://mywebspace.wisc.edu/tdunnick/web)
 *  
 *  This file is part of PhinmsX.
 *
 *  PhinmsX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  PhinmsX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PhinmsX.  If not, see <http://www.gnu.org/licenses/>.
 */

package tdunnick.phinmsx.helper;

import tdunnick.phinmsx.domain.*;
import tdunnick.phinmsx.domain.payload.Hl7Payload;

/**
 * Fesi Helper featuring and HL7 Bar object.
 * 
 * @author Thomas Dunnick
 *
 */
public class FesiHl7Helper extends FesiHelper
{
	Object getPayload(byte[] data)
	{
		return new Hl7Payload (data);
	}
}
