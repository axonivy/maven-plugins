---------------------------------------
Info: When adjusting this test project
---------------------------------------
When adding or changing classes in this folder, 
switch sourceDirectory in test-project-template/pom.xml
to '${basedir}/anotherSrc' and then run 'mvn compile' 
inside the test-project-template and also add the 
generated .class files (in target folder) to revision 
control. 
Afterwards, switch the sourceDirectory back to
the previous value. 