This project consists of extensions for the CDC distributed PHIN-MS (Public 
Health Information Network Messaging System).  It includes a custom receiver 
servlet, a dashboard, a queue monitor, a "mini" HL7 engine, an XML 
content manager, and some examples of "plug in" interfaces for message 
brokering, and other useful tid bits (like a javascript calendar picker).

This is an "ebxml.jar free" version and can stand alone.  A run-ready
WAR file (phinmsx.war) can be found included with the distribution.

This version should be compatible with all versions of PHIN-MS, but has
at this point only been tested with 2.7.0sp1 and 2.8.02.  Plug-ins have
not yet been tested "in situ" (e.g. in an actual PHIN-MS environment), but
the basic receiver is functional including payload decryption.

Installation:

If you simply download the WAR file (phinmsx.war) and place it in the "webapps"
folder of you PHIN-MS distribution it should self configure and be running within
a few seconds.  Point your browser at /phinmsx on your PHIN-MS domain host.  If you
are on the PHIN-MS server the URL will typically be http://localhost:5088/phinmsx.

From there you can read the home page for more information on configuration and
use.  Note that a self configured PhinmsX will create several folders on your
server.  You can of course control where these will ultimately exist through custom
configuration.  Likewise you can secure the application if you like (recommended).

To Do:

The next mile stone for this project would be the creation of a configuration 
utility.  This would best be integrated with the current application (e.g. as
a "configuration" web page).  It would also be handy to provide a GUI (web) interface
to some of the current tools including the password file manager and payload
encryptor.

I'm also always interested in any feedback, especially when running with other
versions of PHIN-MS.

Building:

I have included an Ant build script which you will most likely need to adjust
to your own environment.  All the properties of interest are located at the
top of the script, although some (like JAVA_HOME) are implied.  I built
this within the Eclipse IDE. When built as a "dynamic web" project inclusion 
of the needed jars will generally be the only "tweak" needed (see below).
To keep builds fully compatible with PHIN-MS version be sure to observer the JVM
and servlet specification targets.

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
log4j-1.2.8.jar
sitemesh-2.4.2.jar
standard.jar
xercesImpl.jar
xml-apis.jar
