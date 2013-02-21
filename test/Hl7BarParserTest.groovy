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
 *  Foobar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

import groovy.util.GroovyTestCase;
import tdunnick.phinmsx.minihl7.*;

class Hl7BarParserTest extends GroovyTestCase
{
	Hl7Msg hl7 = null;
	Hl7BarParser bar = null;
	
	String msg = "" +
  "MSH|^~\\&|AHS-SLI|ST ELIZABETH HOSPITAL LAB^^CLIA|vCMR|WEDSS|200907081020||ORU^R01|67d50db3-c111-4247-8099-9807c8a10098|T|2.3.1\n" +
  "PID|1|000000000420894|X311520:999||Kermit^Andrea^T||19781022080001|F||WN^WHITE/NON-HISPANIC^L|2771 Walnut Street^^Unity^WI^54488|||||M^MARRIED^L\n" +
  "NK1|1|Kermit^John^M|H^HUSBAND^L|2771 Walnut Street^^Unity^WI^54488|^^^^^920^7661736|^^^^^920^5853113\n" +
  "ORC|RE|09:B0036303S|09:B0036303S^ST ELIZABETH HOSPITAL LAB^^CLIA||||||200907081020|||YOUB^YOUNG MD^BRETT^D||^^^^^920^9963700|||||||ST ELIZABETH HOSPITAL LAB|1506 S ONEIDA ST^^APPLETON^WI^54915|^^^^^920^7382141|1531 S MADISON ST 4TH FLOOR^^APPLETON^WI^54915\n" +
  "OBR|1|63ZZ00259000|63ZZ00259000^ST ELIZABETH HOSPITAL LAB^^CLIA|^^^107.550^BODY FLUID AEROB CULT RPT^L|||200907061645|||||||200907061703|JF&JOINT FLUID&L|YOUB^YOUNG MD^BRETT^D|^^^^^920^9963700|||09:B0036303S|||||F\n" +
  "OBX|1|ST|^^^107.550^BODY FLUID AEROB CULT RPT^L|1|SPECIMEN COMMENTS:||||||F|||200907080733|SEH^ST ELIZABETH HOSPITAL LAB^L\n" +
  "OBX|2|ST|^^^107.551^BODY FLUID AEROB CULT RPT-1^L|2|PAGE DR YOUNG W/ RESULTS 554-1309||||||F|||200907080733|SEH^ST ELIZABETH HOSPITAL LAB^L\n" +
  "MSH|^~\\&|RD|MAYO CLINIC DEPT. OF LAB MED AND PATHOLOGY^24D0404292^CLIA|WIDOH|WI|200910010408||ORU^R01|2009100104084438632|P|2.3.1\n" +
  "PID|1||X507771:999~X492862:999||Bigbird^Jack||19800701101126|M||U^^HL7 005^^^L\n" +
  "NK1|1\n" +
  "ORC|||||||||||||||||||||THEDACARE LABS|130 2ND ST^^NEENAH^WI^54956|^^^^^920^7292079|130 2ND ST^^NEENAH^WI^54956\n" +
  "OBR|1||33ZZ00874000|^^^81096^CHLAMYDIA TRACHOMATIS AMPLIFIED DNA^L|||200909272345||||||STRAND DISPLACEMENT AMPLIFICATION|200909301202|^^URINE|^CAROTHERS, KELLY||||||200909302341|||F\n" +
  "OBX|1|CE|6357-8^C TRACH DNA UR QL PCR^LN^24075^CHLAMYDIA TRACHOMATIS AMPLIFIED DNA^L||G-A200^POSITIVE^SNM||NEGATIVE||||F|||20090930114100|24D0404292^MAYO CLINIC DEPT. OF LAB MED AND PATHOLOGY^CLIA\n" +
  "NTE|1|C|REPORTABLE DISEASE\n" +
  "MSH|^~\\&|RD|MAYO CLINIC DEPT. OF LAB MED AND PATHOLOGY^24D0404292^CLIA|WIDOH|WI|200910010408||ORU^R01|2009100104084438633|P|2.3.1\n" +
  "PID|1||X728026:999~X201448:999||Labtest^Sebastian||19800112220310|M||U^^HL7 005^^^L\n" +
  "NK1|1\n" +
  "ORC|||||||||||||||||||||DEAN CLINIC|1313 FISH HATCHERY ROAD^^MADISON^WI^53715|^^^^^608^2528025|1313 FISH HATCHERY ROAD^^MADISON^WI^53715\n" +
  "OBR|1||14ZZ00367000|^^^81958^HIV-1 RNA QUANTIFICATION, P^L|||200909291116||||||REVERSE TRANSCRIPTION-POLYMERASE CHAIN REACTION (RT-PCR) (PCR IS UTILIZED PURSUANT TO A LICENSE AGREEMENT WITH ROCHE MOLECULAR SYSTEMS, INC.) NOTE: SEE HIV TREATMENT MONITORING ALGORITHM IN SPECIAL INSTRUCTIONS.|200909300945|^^PLASMA|3310^LEVIN,JAMES M.||||||200909302053|||F\n" +
  "OBX|1|SN|21008-8^HIV 1 RNA^LN^81958^HIV-1 RNA QUANTIFICATION, P^L||^53|COPIES/ML|||||F|||20090930085300|24D1040592^MAYO CLINIC DPT OF LAB MED PATHOLOGY SUPERIOR DR\n" +
  "NTE|1|C\n" +
  "NTE|2||RESULT IN LOG COPIES/ML IS 1.72\n" +
  "NTE|3|C\n" +
  "NTE|4|C|QUANTIFICATION RANGE OF THIS ASSAY IS 48 TO 10,000,000\n" +
  "NTE|5|C|COPIES/ML (1.68 TO 7.00 LOG COPIES/ML).\n" +
  "NTE|6|C\n" +
  "NTE|7|C|TESTING WAS DONE BY THE COBAS AMPLIPREP/COBAS\n" +
  "NTE|8|C|TAQMAN HIV-1 TEST (ROCHE MOLECULAR SYSTEMS, INC.).\n";

	
	public void setUp() throws Exception
	{
		hl7 = new Hl7Msg();
		bar = new Hl7BarParser();
	}

	public void tearDown() throws Exception
	{
	}
	
	public void testEncoding ()
	{
		String f = "field|component^subcomponent&esc\\repeat~";
		String g = bar.encode (f, null);
		assert ! f.equals(g) : "encoding failed"
		// println f + " --> " + g;
		String h = bar.decode (g, null);
		assert h.equals (f) : "decoded " + g + " to " + h
	}
	
	public void testParse()
	{
		int n = bar.parse(hl7, msg);
		assert n == 28 : "parse expected 38 but got " + n + " segments";
	}
	
	public void testFormat ()
	{
		char nl = '\n';
		char cr = '\r'
		bar.parse(hl7, msg);
		String s = bar.format (hl7, null);
		// println (s);
		assert msg.replace(nl, cr).equals (s) : "message didn't match : " + s
	}

	public void testPath()
	{
		bar.parse(hl7, msg);
		String s = hl7.get ("OBX:2-3-4");
		assert s != null : "get OBX:2-3-4 returned null";
		assert s.equals("107.551") : "expect 107.551 but got " + s;
		s = hl7.get ("OBX:1-3-4");
		assert s != null : "get OBX:1-3-4 returned null";
		assert s.equals("107.550") : "expect 107.550 but got " + s;
		s = hl7.get ("MSH:3-7");
		assert s != null : "get MSH:3-7 returned null";
		assert s.equals("200910010408") : "expect 200910010408 but got " + s;		
	}
	
	public void testSegPath ()
	{
		bar.parse(hl7, msg);
		String s = hl7.segPath (6);
		assert s.equals ("OBX:2") : "expect OBX:2 but got " + s
		s = hl7.segPath (26);
		assert s.equals ("NTE:8") : "expect NTE:8 but got " + s
		
	}
}
