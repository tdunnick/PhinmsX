This project consists of extensions for the CDC distributed PHIN-MS (Public 
Health Information Network Messaging System).  It includes a custom receiver 
servlet, a dashboard, a queue monitor, a "mini" HL7 engine, an XML 
content manager, and some examples of "plug in" interfaces for message 
brokering, and other useful tid bits (like a javascript calendar picker).

As such it is currently dependent on libraries unique to that application 
which while "free" is not freely distributable.  You should contact the CDC 
to obtain a copy of PHIN-MS (http://www.cdc.gov/phin/tools/PHINms/).  

This version is compatible with 2.7.x and 2.8.01 versions of PHIN-MS.  It 
breaks beginning with 2.8.01sp1 where the PHIN-MS API changes.  At some point
a "PHIN-MS free" version may be developed.  Replacement API's are welcomed.

I have not included any build scripts, mainly because most of you will
build this within an IDE which will have it's own idiosyncratic
build process. When built as a "dynamic web" project inclusion of the needed
jars will generally be the only "tweek" needed (see below).

Also note the unit tests are under a separate source folder and are
written in Groovy, rather than pure Java.  This allows the unit tests to
access and test private methods through Groovy reflection.  You need to
configure "groovy nature" and add junit.jar to build and run the unit tests.

This project is licensed under the GPL (Gnu Public License).  See 
COPYING.txt and License.txt for details.  Other applicable notices are 
included.

libs from PHIN-MS needed to build...

ebxml.jar (NOTE - this is the only jar with distribution restricted by CDC)
bcprov-jdk16-138.jar
commons-codec-1.3.jar
hsqldb.jar
log4j-1.2.8.jar

general libs needed for support...

jcommon-1.0.17.jar
jfreechart-1.0.14.jar
jstl.jar
sitemesh-2.4.2.jar
standard.jar
fesi.jar (if you want ECMA Script support)

libs from PHIN-MS needed to run

mail.jar
dom4j.jar
xss4j.jar
xercesImpl.jar
xalan.jar