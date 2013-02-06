This project consists of extensions for the CDC distributed PHIN-MS (Public 
Health Information Network Messaging System).  It includes a custom receiver 
servlet, a dashboard, a queue monitor, a "mini" HL7 engine, an XML 
content manager, and some examples of "plug in" interfaces for message 
brokering, and other useful tid bits (like a javascript calendar picker).

This is the first "ebxml.jar free" version and can stand alone.  A run-ready
WAR file should be available in the near future.

This version should be compatible with all versions of PHIN-MS, but has
at this point only been partially tested with 2.7.0sp1.  Additional testing
up through 2.8.02 will be done with the next release.

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

libs needed...

bcprov-jdk16-138.jar
dom4j.jar
fesi.jar (if you want ECMA Script support)
hsqldb.jar
jcommon-1.0.17.jar
jfreechart-1.0.14.jar
jstl.jar
log4j.jar
sitemesh-2.4.2.jar
standard.jar
xercesImpl.jar
