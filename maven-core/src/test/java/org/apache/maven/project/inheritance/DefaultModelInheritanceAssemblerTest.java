/* Created on Aug 23, 2004 */
package org.apache.maven.project.inheritance;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.PostGoal;
import org.apache.maven.model.PreGoal;
import org.apache.maven.model.Resource;
import org.apache.maven.model.UnitTest;

import java.util.Arrays;
import java.util.List;

/**
 * @author jdcasey
 */
public class DefaultModelInheritanceAssemblerTest
    extends TestCase
{
    public void testShouldOverrideUnitTestExcludesOnly()
    {
        Model parent = new Model();

        parent.setGroupId( "test" );
        parent.setArtifactId( "test" );
        parent.setVersion( "0.0" );

        Build parentBuild = new Build();

        parentBuild.setSourceDirectory( "src/main/java" );
        parentBuild.setAspectSourceDirectory( "src/main/aspects" );
        parentBuild.setUnitTestSourceDirectory( "src/test/java" );

        Resource parentResource = new Resource();

        parentResource.setDirectory( "src/main/resources" );

        parentBuild.addResource( parentResource );

        UnitTest parentUT = new UnitTest();

        parentUT.setIncludes( Arrays.asList( new String[] { "**/*Test.java" } ) );
        parentUT.setExcludes( Arrays.asList( new String[] { "**/*Abstract*.java" } ) );

        Resource parentUTResource = new Resource();

        parentUTResource.setDirectory( "src/test/resources" );

        parentUT.addResource( parentUTResource );

        parentBuild.setUnitTest( parentUT );
        parent.setBuild( parentBuild );
        
        PreGoal preGoal1 = new PreGoal();
        preGoal1.setName("compiler:compile");
        preGoal1.setAttain("clean:clean");
        
        parent.addPreGoal(preGoal1);
        
        // hehe...try getting anything done with this one in place!
        PostGoal postGoal1 = new PostGoal();
        postGoal1.setName("jar:jar");
        postGoal1.setAttain("clean:clean");
        
        parent.addPostGoal(postGoal1);

        Model child = new Model();
        
        child.setType( "plugin" );

        Build childBuild = new Build();

        UnitTest childUT = new UnitTest();

        parentUT.setExcludes( Arrays.asList( new String[] { "**/*Abstract*.java", "**/*AspectTest.java" } ) );

        childBuild.setUnitTest( childUT );
        child.setBuild( childBuild );
        
        PreGoal preGoal2 = new PreGoal();
        preGoal2.setName("compiler:compile");
        preGoal2.setAttain("qdox:generate");
        
        child.addPreGoal(preGoal2);

        ModelInheritanceAssembler assembler = new DefaultModelInheritanceAssembler();

        assembler.assembleModelInheritance( child, parent );

        assertEquals( "source directory should be from parent", "src/main/java", child.getBuild().getSourceDirectory() );
        assertEquals( "unit test source directory should be from parent", "src/test/java", child.getBuild()
            .getUnitTestSourceDirectory() );
        assertEquals( "aspect source directory should be from parent", "src/main/aspects", child.getBuild()
            .getAspectSourceDirectory() );

        List childExcludesTest = child.getBuild().getUnitTest().getExcludes();

        assertTrue( "unit test excludes should have **/*AspectTest.java", childExcludesTest
            .contains( "**/*AspectTest.java" ) );
        assertTrue( "unit test excludes should have **/*Abstract*.java", childExcludesTest
            .contains( "**/*Abstract*.java" ) );

        List resources = child.getBuild().getResources();

        assertEquals( "build resources inherited from parent should be of size 1", 1, resources.size() );
        assertEquals( "first resource should have dir == src/main/resources", "src/main/resources",
            ((Resource) resources.get( 0 )).getDirectory() );

        List utResources = child.getBuild().getUnitTest().getResources();

        assertEquals( "UT resources inherited from parent should be of size 1", 1, utResources.size() );
        assertEquals( "first UT resource should have dir == src/test/resources", "src/test/resources",
            ((Resource) utResources.get( 0 )).getDirectory() );

        assertEquals( "plugin", child.getType() );
        assertEquals( "jar", parent.getType() );
        
        assertEquals("merged child should have 2 preGoals", 2, child.getPreGoals().size());
        assertTrue("preGoal should be inherited from parent", child.getPreGoals().contains(preGoal1));
        assertTrue("preGoal should be preserved from child", child.getPreGoals().contains(preGoal2));
        
        assertEquals("1 postGoal should be inherited from parent", 1, child.getPostGoals().size());
    }
}