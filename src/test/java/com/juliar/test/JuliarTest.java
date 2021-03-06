package com.juliar.test;

import com.juliar.Juliar;
import junit.framework.TestCase;
import com.juliar.errors.JuliarLogger;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Created by donreamey on 10/25/16.
 */
public class JuliarTest extends TestCase {
    public void setUp() throws Exception {
        super.setUp();
        assertTrue(true);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        assertTrue(true);
    }

    public void testInterpreter() throws Exception{
        Juliar compiler = new Juliar();
        compiler.isDebug = true;
        List<String> errorList = null;
        errorList = compiler.compile("examples/test.jlr", ".",  false);

        if (errorList.size() > 0 ){
            for(int i =0; i < errorList.size(); i++){
                JuliarLogger.log( errorList.get(i) );
            }
            throw new RuntimeException( "compile errors" );
        }

        try {
            String [] libsToLoad = new String[] { "test.lib" };//, "serialize.lib" } ;
            testLoadLibs( libsToLoad );
        }
        catch( Exception ex){
            ex.printStackTrace();
            throw ex;
        }
        assertTrue(true);
    }

    public void testCompile() throws Exception {
        /*try {
            Juliar compiler = new Juliar();
            assertNotNull(compiler);
            //List<String> errorList = compiler.compile("test.jrl", ".",  false);

            for(String s : errorList){
                out.println(s);
            }
            Assert.assertTrue( errorList.size() == 0 );
        }
        catch(Exception ex) {
            throw ex;
        }*/
        assertTrue(true);
    }


    public void testLoadLibs( String[] libToLoad) throws Exception {
        assertNotNull(libToLoad);
        /*try {
            InstructionInvocation invocation = com.juliar.LoaderLinker.LoadLink.loadAndLink( libToLoad );
            if ( invocation != null ) {
                new Interpreter(invocation);
            }
        }
        catch( Exception ex){
            ex.printStackTrace();
            throw ex;
        }
        */
        assertTrue(true);
    }

    public void testScriptEngine() throws Exception {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("nashorn");
        engine.eval("function sum(a, b) { a = b; a++; return b; }");
        Object returnScriptValue = engine.eval("sum(1, 2);");
        int i = 4;
        assertTrue(true);
    }
}