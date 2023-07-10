package com.google.gdt.eclipse.designer.core.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.designer.Activator;
import com.google.gdt.eclipse.designer.IExceptionConstants;
import com.google.gdt.eclipse.designer.common.Constants;
import com.google.gdt.eclipse.designer.core.GTestUtils;
import com.google.gdt.eclipse.designer.model.module.ModuleElement;
import com.google.gdt.eclipse.designer.util.DefaultModuleDescription;
import com.google.gdt.eclipse.designer.util.DefaultModuleProvider;
import com.google.gdt.eclipse.designer.util.DefaultModuleProvider.ModuleModification;
import com.google.gdt.eclipse.designer.util.IModuleFilter;
import com.google.gdt.eclipse.designer.util.ModuleDescription;
import com.google.gdt.eclipse.designer.util.Utils;
import org.eclipse.wb.internal.core.utils.Version;
import org.eclipse.wb.internal.core.utils.ast.DomGenerics;
import org.eclipse.wb.internal.core.utils.exception.DesignerException;
import org.eclipse.wb.internal.core.utils.exception.DesignerExceptionUtils;
import org.eclipse.wb.internal.core.utils.jdt.core.ProjectUtils;
import org.eclipse.wb.tests.designer.TestUtils;
import org.eclipse.wb.tests.designer.core.AbstractJavaTest;
import org.eclipse.wb.tests.designer.core.TestProject;
import org.eclipse.wb.tests.designer.core.annotations.DisposeProjectAfter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import static org.fest.assertions.Assertions.assertThat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Test for {@link Utils}.
 * 
 * @author scheglov_ke
 */
public class UtilsTest extends AbstractJavaTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (m_testProject == null) {
            do_projectCreate();
            GTestUtils.configure(m_testProject);
            GTestUtils.createModule(m_testProject, "test.Module");
            waitForAutoBuild();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        Activator.getStore().setValue(Constants.P_GWT_LOCATION, GTestUtils.getLocation());
        super.tearDown();
    }

    public void _test_exit() throws Exception {
        System.exit(0);
    }

    /**
   * Test for {@link Utils#hasGPE()}.
   */
    public void test_hasGPE() throws Exception {
        assertTrue(Utils.hasGPE());
    }

    /**
   * Test for {@link Utils#hasGPE()}.
   */
    public void test_hasGPE_falseInTesting() throws Exception {
        System.setProperty("wbp.noGPE", "true");
        try {
            assertFalse(Utils.hasGPE());
        } finally {
            System.clearProperty("wbp.noGPE");
        }
    }

    /**
   * Test for {@link Utils#getGWTLocation(IProject)}.<br>
   * Absolute path to the <code>gwt-user.jar</code> in classpath.
   */
    public void test_getGWTLocation_fromAbsolute() throws Exception {
        assertEquals(GTestUtils.getLocation(), Utils.getGWTLocation(m_project));
        assertEquals(GTestUtils.getLocation() + "/gwt-user.jar", Utils.getUserLibPath(m_project).toPortableString());
    }

    /**
   * Test for {@link Utils#getGWTLocation(IProject)}.<br>
   * <code>null</code> as {@link IProject}, so default GWT location used.
   */
    public void test_getGWTLocation_nullProject() throws Exception {
        Activator.getStore().setValue(Constants.P_GWT_LOCATION, "/some/folder");
        assertEquals("/some/folder", Utils.getGWTLocation(null));
        assertEquals("/some/folder/gwt-user.jar", Utils.getUserLibPath(null).toPortableString());
    }

    /**
   * Test for {@link Utils#getGWTLocation(IProject)}.<br>
   * Use <code>GWT_HOME</code> variable in classpath.
   */
    @DisposeProjectAfter
    public void test_getGWTLocation_from_GWT_HOME() throws Exception {
        do_projectDispose();
        do_projectCreate();
        {
            IJavaProject javaProject = m_testProject.getJavaProject();
            IClasspathEntry entry = JavaCore.newVariableEntry(new Path("GWT_HOME/gwt-user.jar"), null, null);
            ProjectUtils.addClasspathEntry(javaProject, entry);
        }
        String location_20 = GTestUtils.getLocation_20();
        Activator.getStore().setValue(Constants.P_GWT_LOCATION, location_20);
        assertEquals(location_20, Utils.getGWTLocation(m_project));
        assertEquals(location_20 + "/gwt-user.jar", Utils.getUserLibPath(m_project).toPortableString());
    }

    /**
   * Test for {@link Utils#getGWTLocation(IProject)}.<br>
   * <code>gwt-user.jar</code> has different name.
   */
    @DisposeProjectAfter
    public void test_getGWTLocation_otherUserName() throws Exception {
        do_projectDispose();
        do_projectCreate();
        File gwtUserFile;
        {
            gwtUserFile = File.createTempFile("gwtUser_", ".jar").getCanonicalFile();
            gwtUserFile.deleteOnExit();
            FileUtils.copyFile(new File(GTestUtils.getLocation() + "/gwt-user.jar"), gwtUserFile);
        }
        File gwtDevFile;
        {
            gwtDevFile = new File(gwtUserFile.getParentFile(), "gwt-dev.jar");
            gwtDevFile.deleteOnExit();
            FileUtils.copyFile(new File(GTestUtils.getLocation() + "/gwt-dev.jar"), gwtDevFile);
        }
        {
            IJavaProject javaProject = m_testProject.getJavaProject();
            IClasspathEntry entry = JavaCore.newLibraryEntry(new Path(gwtUserFile.getAbsolutePath()), null, null);
            ProjectUtils.addClasspathEntry(javaProject, entry);
        }
        {
            String expected = new Path(gwtUserFile.getParent()).toPortableString();
            assertEquals(expected, Utils.getGWTLocation(m_project));
        }
        {
            String expected = new Path(gwtUserFile.getAbsolutePath()).toPortableString();
            assertEquals(expected, Utils.getUserLibPath(m_project).toPortableString());
        }
        {
            {
                String expected = new Path(gwtDevFile.getAbsolutePath()).toPortableString();
                assertEquals(expected, Utils.getDevLibPath(m_project).toPortableString());
            }
            {
                gwtDevFile.delete();
                String expected = GTestUtils.getLocation() + "/gwt-dev.jar";
                assertEquals(expected, Utils.getDevLibPath(m_project).toPortableString());
            }
        }
    }

    /**
   * Test for {@link Utils#getGWTLocation(IProject)}.<br>
   * Not a GWT project.
   */
    @DisposeProjectAfter
    public void test_getGWTLocation_notGWT() throws Exception {
        do_projectDispose();
        do_projectCreate();
        assertNull(Utils.getGWTLocation(m_project));
        assertNull(Utils.getUserLibPath(m_project));
    }

    /**
   * Test for {@link Utils#getUserLibPath(IProject)}.
   */
    public void test_getUserLibPath() throws Exception {
        assertEquals(GTestUtils.getLocation() + "/gwt-user.jar", Utils.getUserLibPath(m_project).toPortableString());
    }

    /**
   * Test for {@link Utils#getDevLibPath(IProject)}. After GWT 2.0 version.
   */
    @DisposeProjectAfter
    public void test_getDevLibPath_after20() throws Exception {
        do_projectDispose();
        do_projectCreate();
        GTestUtils.configure(GTestUtils.getLocation_21(), m_testProject);
        assertEquals(GTestUtils.getLocation_21() + "/gwt-dev.jar", Utils.getDevLibPath(m_project).toPortableString());
    }

    /**
   * Test for {@link Utils#getDevLibPath(IProject)}. For Maven.
   * <p>
   * http://fogbugz.instantiations.com/fogbugz/default.asp?48259
   */
    @DisposeProjectAfter
    public void test_getDevLibPath_maven() throws Exception {
        do_projectDispose();
        do_projectCreate();
        String gwtLocation = GTestUtils.getLocation_22();
        String gwtUserDir = getFolder("libs/gwt/gwt-user/2.2.0").getLocation().toPortableString();
        String gwtDevDir = getFolder("libs/gwt/gwt-dev/2.2.0").getLocation().toPortableString();
        String userLocation = gwtUserDir + "/gwt-user-2.2.0.jar";
        String devLocation = gwtDevDir + "/gwt-dev-2.2.0.jar";
        FileUtils.copyFile(new File(gwtLocation, "gwt-user.jar"), new File(userLocation), false);
        FileUtils.copyFile(new File(gwtLocation, "gwt-dev.jar"), new File(devLocation), false);
        ProjectUtils.addExternalJar(m_javaProject, userLocation, null);
        m_project.refreshLocal(IResource.DEPTH_INFINITE, null);
        assertEquals(devLocation, Utils.getDevLibPath(m_project).toPortableString());
    }

    /**
   * Test for {@link Utils#getDefaultVersion()}.
   */
    public void test_getDefaultVersion() throws Exception {
        check_getDefaultVersion(GTestUtils.getLocation_20(), Utils.GWT_2_0);
        check_getDefaultVersion(GTestUtils.getLocation_2_1_0(), Utils.GWT_2_1);
        check_getDefaultVersion(GTestUtils.getLocation_21(), Utils.GWT_2_1_1);
        check_getDefaultVersion(GTestUtils.getLocation_22(), Utils.GWT_2_2);
        check_getDefaultVersion(GTestUtils.getLocation_24(), Utils.GWT_2_4);
        check_getDefaultVersion("", Utils.GWT_2_4);
    }

    /**
   * Checks {@link Utils#getDefaultVersion()}.
   */
    private static void check_getDefaultVersion(String gwtLocation, Version expected) throws Exception {
        String oldLocation = Activator.getGWTLocation();
        try {
            Activator.setGWTLocation(gwtLocation);
            Version actual = Utils.getDefaultVersion();
            assertEquals(expected, actual);
        } finally {
            Activator.setGWTLocation(oldLocation);
        }
    }

    /**
   * Test for {@link Utils#getVersion(IJavaProject)}.
   */
    @DisposeProjectAfter
    public void test_getVersion() throws Exception {
        check_getVersion(GTestUtils.getLocation_20(), Utils.GWT_2_0);
        check_getVersion(GTestUtils.getLocation_2_1_0(), Utils.GWT_2_1);
        check_getVersion(GTestUtils.getLocation_21(), Utils.GWT_2_1_1);
        check_getVersion(GTestUtils.getLocation_22(), Utils.GWT_2_2);
        check_getVersion(GTestUtils.getLocation_24(), Utils.GWT_2_4);
        check_getVersion("", Utils.GWT_2_4);
    }

    /**
   * Checks {@link Utils#getVersion(IJavaProject)} and {@link Utils#getVersion(IProject)}.
   */
    private void check_getVersion(String gwtLocation, Version expected) throws Exception {
        try {
            do_projectDispose();
            do_projectCreate();
            GTestUtils.configure(gwtLocation, m_testProject);
            {
                Version actual = Utils.getVersion(m_javaProject);
                assertEquals(expected, actual);
            }
            {
                Version actual = Utils.getVersion(m_project);
                assertEquals(expected, actual);
            }
        } finally {
            do_projectDispose();
        }
    }

    /**
   * Test for {@link Utils#getExactModule(Object)}.
   */
    public void test_getExactModule() throws Exception {
        {
            IFile file = getFile(".project");
            assertNull(Utils.getExactModule(file));
        }
        {
            IFile file = getFileSrc("/test/Module.gwt.xml");
            assertNotNull(Utils.getExactModule(file));
        }
    }

    /**
   * Test for {@link Utils#getSimpleModuleName(IFile)}.
   */
    public void test_getSimpleModuleName() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        assertEquals("Module", module.getSimpleName());
    }

    /**
   * Test for {@link Utils#getModuleId(IFile)}.<br>
   * Module file in package.
   */
    public void test_getModuleId_1() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        assertEquals("test.Module", module.getId());
    }

    /**
   * Test for {@link Utils#getModuleId(IFile)}.<br>
   * Module file in root of source folder.
   */
    public void test_getModuleId_2() throws Exception {
        IFile file = setFileContentSrc("TopLevel.gwt.xml", "");
        ModuleDescription module = new DefaultModuleDescription(file);
        assertEquals("TopLevel", module.getId());
    }

    /**
   * Test for {@link Utils#getModule(IJavaProject, String)}.
   */
    public void test_getModule() throws Exception {
        IJavaProject javaProject = m_testProject.getJavaProject();
        assertModuleDescriptionPath("src/test/Module.gwt.xml", Utils.getModule(javaProject, "test.Module"));
        assertNull(Utils.getModule(javaProject, "no.such.Module"));
    }

    /**
   * Test for {@link Utils#getModules(IJavaProject)}.<br>
   * Single default module.
   */
    public void test_getModules_inProject_1() throws Exception {
        IJavaProject javaProject = m_testProject.getJavaProject();
        List<ModuleDescription> modules = Utils.getModules(javaProject);
        assertThat(modules).hasSize(1);
        assertModuleDescriptionPath("src/test/Module.gwt.xml", modules.get(0));
    }

    /**
   * Test for {@link Utils#getModules(IJavaProject)}.<br>
   * Default module + new module.
   */
    @DisposeProjectAfter
    public void test_getModules_inProject_2() throws Exception {
        GTestUtils.createModule(m_testProject, "second.MyModule");
        IJavaProject javaProject = m_testProject.getJavaProject();
        List<ModuleDescription> moduleFiles = Utils.getModules(javaProject);
        assertThat(moduleFiles).hasSize(2);
        assertModuleDescriptionPath("src/second/MyModule.gwt.xml", moduleFiles.get(0));
        assertModuleDescriptionPath("src/test/Module.gwt.xml", moduleFiles.get(1));
    }

    /**
   * Test for {@link Utils#inheritsModule(IFile, String)}.
   */
    @DisposeProjectAfter
    public void test_inheritsModule() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        assertFalse(Utils.inheritsModule(module, "second.MyModule"));
        GTestUtils.createModule(m_testProject, "second.MyModule");
        assertFalse(Utils.inheritsModule(module, "second.MyModule"));
        DefaultModuleProvider.modify(module, new ModuleModification() {

            public void modify(ModuleElement moduleElement) throws Exception {
                moduleElement.addInheritsElement("second.MyModule");
            }
        });
        assertTrue(Utils.inheritsModule(module, "second.MyModule"));
    }

    /**
   * Test for {@link Utils#getSingleModule(IResource)}.
   */
    @DisposeProjectAfter
    public void test_getSingleModule_IResource() throws Exception {
        {
            {
                IResource resource = getFile(".project");
                assertNull(Utils.getSingleModule(resource));
            }
            {
                IResource resource = m_project;
                assertNull(Utils.getSingleModule(resource));
            }
            {
                IFolder folder = ensureFolderExists("someFolder");
                assertNull(Utils.getSingleModule(folder));
            }
            {
                IResource resource = setFileContentSrc("test2/Test.java", getSourceDQ("// filler filler filler filler filler", "// filler filler filler filler filler", "package test2;", "public class Test {", "}"));
                assertNull(Utils.getSingleModule(resource));
            }
        }
        {
            {
                IResource resource = getFileSrc("test/Module.gwt.xml");
                assertModuleDescriptionPath("src/test/Module.gwt.xml", Utils.getSingleModule(resource));
            }
            {
                IResource resource = getFolderSrc("test/client");
                assertModuleDescriptionPath("src/test/Module.gwt.xml", Utils.getSingleModule(resource));
            }
        }
    }

    /**
   * Test for {@link Utils#getSingleModule(ICompilationUnit)}.
   */
    public void test_getSingleModule_ICompilationUnit() throws Exception {
        IType entryPointType = m_testProject.getJavaProject().findType("test.client.Module");
        ICompilationUnit compilationUnit = entryPointType.getCompilationUnit();
        assertModuleDescriptionPath("src/test/Module.gwt.xml", Utils.getSingleModule(compilationUnit));
    }

    /**
   * Test for {@link Utils#getSingleModule(IPackageFragment)}.
   */
    public void test_getSingleModule_IPackageFragment() throws Exception {
        IType entryPointType = m_testProject.getJavaProject().findType("test.client.Module");
        IPackageFragment packageFragment = entryPointType.getPackageFragment();
        assertModuleDescriptionPath("src/test/Module.gwt.xml", Utils.getSingleModule(packageFragment));
    }

    /**
   * Test for {@link Utils#getSingleModule(IType)}.
   */
    public void test_getSingleModule_IType() throws Exception {
        IType entryPointType = m_testProject.getJavaProject().findType("test.client.Module");
        assertModuleDescriptionPath("src/test/Module.gwt.xml", Utils.getSingleModule(entryPointType));
    }

    /**
   * Test for {@link Utils#getSingleModule(ICompilationUnit)}.
   * <p>
   * Uses <code>gwtd.module.use</code> marker to force using marked module.
   */
    @DisposeProjectAfter
    public void test_getSingleModule_useMarker() throws Exception {
        getTestModuleFile().delete(true, null);
        setFileContentSrc("test/aModule.gwt.xml", "<module/>");
        setFileContentSrc("test/bModule.gwt.xml", "<module/> <!-- gwtd.module.use -->");
        setFileContentSrc("test/cModule.gwt.xml", "<module/>");
        IFolder folder = getFolderSrc("test");
        ModuleDescription module = Utils.getSingleModule(folder);
        assertEquals("test.bModule", module.getId());
    }

    /**
   * Test for {@link Utils#getSingleModule(ICompilationUnit)}.
   * <p>
   * Uses {@link IModuleFilter} to keep "second" module alive.
   */
    @DisposeProjectAfter
    public void test_getSingleModule_IModuleFilter() throws Exception {
        getTestModuleFile().delete(true, null);
        setFileContentSrc("test/aModule.gwt.xml", "<module/>");
        setFileContentSrc("test/bModule.gwt.xml", "<module/>");
        String extPointId = "com.google.gdt.eclipse.designer.moduleProviders";
        try {
            TestUtils.setContributionBundle(com.google.gdt.eclipse.designer.tests.Activator.getDefault().getBundle());
            TestUtils.addDynamicExtension(extPointId, "<filter class='" + MyModuleFilter.class.getName() + "'/>");
            IFolder folder = getFolderSrc("test");
            ModuleDescription module = Utils.getSingleModule(folder);
            assertEquals("test.bModule", module.getId());
        } finally {
            TestUtils.removeDynamicExtension(extPointId);
            TestUtils.setContributionBundle(null);
        }
    }

    /**
   * Removes "test.aModule" module.
   */
    public static final class MyModuleFilter implements IModuleFilter {

        @Override
        public List<ModuleDescription> filter(List<ModuleDescription> modules) throws Exception {
            List<ModuleDescription> filtered = Lists.newArrayList();
            for (ModuleDescription module : modules) {
                if (!module.getId().equals("test.aModule")) {
                    filtered.add(module);
                }
            }
            return filtered;
        }
    }

    /**
   * Test for {@link Utils#getSingleModule(IResource)}.
   * <p>
   * Maven-like project. Module file in "resources".
   */
    @DisposeProjectAfter
    public void test_getSingleModule_maven_1() throws Exception {
        GTestUtils.configureMavenProject();
        {
            IResource resource = getFolder("src/main/java/test/client");
            ModuleDescription module = Utils.getSingleModule(resource);
            assertModuleDescriptionPath("src/main/resources/test/Module.gwt.xml", module);
        }
        {
            IResource resource = setFileContent("src/main/resources/test/client/MyResource.txt", "");
            ModuleDescription module = Utils.getSingleModule(resource);
            assertModuleDescriptionPath("src/main/resources/test/Module.gwt.xml", module);
        }
    }

    /**
   * Test for {@link Utils#getSingleModule(IResource)}.
   * <p>
   * Maven-like project. Module file in "java".
   */
    @DisposeProjectAfter
    public void test_getSingleModule_maven_2() throws Exception {
        GTestUtils.configureMavenProject();
        {
            IFile moduleFile = getFile("src/main/resources/test/Module.gwt.xml");
            moduleFile.move(new Path("/TestProject/src/main/java/test/Module.gwt.xml"), true, null);
        }
        {
            IResource resource = m_project;
            ModuleDescription module = Utils.getSingleModule(resource);
            assertNull(module);
        }
        {
            IResource resource = getFolder("src/main/java/test/client");
            ModuleDescription module = Utils.getSingleModule(resource);
            assertModuleDescriptionPath("src/main/java/test/Module.gwt.xml", module);
        }
        {
            IResource resource = setFileContent("src/main/resources/test/client/MyResource.txt", "");
            ModuleDescription module = Utils.getSingleModule(resource);
            assertModuleDescriptionPath("src/main/java/test/Module.gwt.xml", module);
        }
    }

    /**
   * Test for {@link ModuleDescription#getModuleFolder()} .
   */
    public void test_getModuleFolder() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        assertResourcePath("src/test", module.getModuleFolder());
    }

    /**
   * Test for {@link ModuleDescription#getModulePackage()}.
   */
    public void test_getModulePackage() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        assertEquals("test", module.getModulePackage().getElementName());
    }

    /**
   * Test for {@link ModuleDescription#getModulePublicFolder()}.
   */
    public void test_getModulePublicFolder_1() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        IResource publicFolder = module.getModulePublicFolder();
        assertResourcePath("src/test/public", publicFolder);
    }

    /**
   * Test for {@link ModuleDescription#getModulePublicFolder()}.
   */
    @DisposeProjectAfter
    public void test_getModulePublicFolder_2() throws Exception {
        IFile moduleFile = getFileSrc("test/Module.gwt.xml");
        m_testProject.getPackage("test.myPublicFolder");
        setFileContent(moduleFile, getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <public path='myPublicFolder'/>", "</module>"));
        ModuleDescription module = getTestModuleDescription();
        IResource publicFolder = module.getModulePublicFolder();
        assertResourcePath("src/test/myPublicFolder", publicFolder);
    }

    /**
   * Test for {@link Utils#getRootSourcePackage(IPackageFragment)}.<br>
   * Ask for "client" package itself.
   */
    public void test_getRootSourcePackage_1() throws Exception {
        IPackageFragment pkg = m_testProject.getPackage("test.client");
        IPackageFragment root = Utils.getRootSourcePackage(pkg);
        assertEquals("test.client", root.getElementName());
    }

    /**
   * Test for {@link Utils#getRootSourcePackage(IPackageFragment)}.<br>
   * Ask for child of "client" package.
   */
    @DisposeProjectAfter
    public void test_getRootSourcePackage_2() throws Exception {
        IPackageFragment pkg = m_testProject.getPackage("test.client.rpc");
        IPackageFragment root = Utils.getRootSourcePackage(pkg);
        assertEquals("test.client", root.getElementName());
    }

    /**
   * Test for {@link Utils#isModuleSourcePackage(IPackageFragment)}.
   */
    @DisposeProjectAfter
    public void test_isModuleSourcePackage() throws Exception {
        {
            IPackageFragment pkg = m_testProject.getPackage("test.server");
            assertFalse(Utils.isModuleSourcePackage(pkg));
        }
        {
            IPackageFragment pkg = m_testProject.getPackage("test.client");
            assertTrue(Utils.isModuleSourcePackage(pkg));
        }
        {
            IPackageFragment pkg = m_testProject.getPackage("test.newClient");
            assertFalse(Utils.isModuleSourcePackage(pkg));
            {
                IFile moduleFile = getFileSrc("test/Module.gwt.xml");
                setFileContent(moduleFile, getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <source path='newClient'/>", "</module>"));
                waitForAutoBuild();
            }
            assertTrue(Utils.isModuleSourcePackage(pkg));
        }
    }

    /**
   * Test for {@link Utils#isModuleSourcePackage(IPackageFragment)}.
   */
    @DisposeProjectAfter
    public void test_isModuleSourcePackage_withExcludeElements() throws Exception {
        IPackageFragment inClientPkg = m_testProject.getPackage("test.client.foo");
        IPackageFragment theServicePkg = m_testProject.getPackage("test.client.foo.service");
        IPackageFragment inServicePkg = m_testProject.getPackage("test.client.foo.service.bar");
        {
            IFile moduleFile = getFileSrc("test/Module.gwt.xml");
            setFileContent(moduleFile, getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <source path='client'>", "    <exclude name='**/service/**'/>", "  </source>", "</module>"));
            waitForAutoBuild();
        }
        assertTrue(Utils.isModuleSourcePackage(inClientPkg));
        assertFalse(Utils.isModuleSourcePackage(theServicePkg));
        assertFalse(Utils.isModuleSourcePackage(inServicePkg));
    }

    /**
   * Test for {@link Utils#isModuleSourcePackage(IPackageFragment)}.
   */
    @DisposeProjectAfter
    public void test_isModuleSourcePackage_withRenameTo() throws Exception {
        IPackageFragment inClientPkg = m_testProject.getPackage("test.client");
        IPackageFragment inServerPkg = m_testProject.getPackage("test.server");
        {
            IFile moduleFile = getFileSrc("test/Module.gwt.xml");
            setFileContent(moduleFile, "<module rename-to='shortName'/>");
            waitForAutoBuild();
        }
        assertTrue(Utils.isModuleSourcePackage(inClientPkg));
        assertFalse(Utils.isModuleSourcePackage(inServerPkg));
    }

    /**
   * Test for {@link Utils#isModuleSourcePackage(IPackageFragment)}.
   * <p>
   * If module inherits from other module, then it includes its source/client packages.
   * <p>
   * http://code.google.com/p/google-web-toolkit/issues/detail?id=6626
   */
    @DisposeProjectAfter
    public void test_isModuleSourcePackage_withInherits() throws Exception {
        IPackageFragment inClientPkg = m_testProject.getPackage("test.webclient");
        IPackageFragment inServerPkg = m_testProject.getPackage("test.server");
        getFileSrc("test/Module.gwt.xml").delete(true, null);
        setFileContentSrc("test/ModuleB.gwt.xml", getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <source path='webclient'/>", "</module>"));
        setFileContentSrc("test/ModuleA.gwt.xml", getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <inherits name='test.ModuleB'/>", "</module>"));
        assertTrue(Utils.isModuleSourcePackage(inClientPkg));
        assertFalse(Utils.isModuleSourcePackage(inServerPkg));
    }

    /**
   * Test for {@link Utils#isModuleSourcePackage(IPackageFragment)}.
   * <p>
   * http://forums.instantiations.com/viewtopic.php?f=11&t=5300
   */
    @DisposeProjectAfter
    public void test_isModuleSourcePackage_maven() throws Exception {
        getFolder("src").delete(true, null);
        getFolder("src/main/java");
        getFolder("src/main/resources");
        m_testProject.removeSourceFolder("/TestProject/src");
        m_testProject.addSourceFolder("/TestProject/src/main/java");
        m_testProject.addSourceFolder("/TestProject/src/main/resources");
        waitForAutoBuild();
        GTestUtils.createModule(m_testProject, "test.Module");
        {
            IFile moduleFile = getFile("src/main/java/test/Module.gwt.xml");
            getFolder("src/main/resources/test");
            moduleFile.move(new Path("/TestProject/src/main/resources/test/Module.gwt.xml"), true, null);
        }
        {
            IPackageFragment pkg = m_testProject.getPackage("test.client");
            assertTrue(Utils.isModuleSourcePackage(pkg));
        }
    }

    /**
   * Test for {@link Utils#readModule(IFile)}, without problems.
   */
    public void test_readModule_IFile_OK() throws Exception {
        ModuleDescription moduleDescription = getTestModuleDescription();
        ModuleElement moduleElement = Utils.readModule(moduleDescription);
        assertEquals("test.Module", moduleElement.getName());
    }

    /**
   * Test for {@link Utils#readModule(IFile)}, with problems.
   */
    @DisposeProjectAfter
    public void test_readModule_IFile_bad() throws Exception {
        setFileContentSrc("test/Module.gwt.xml", "<module>");
        try {
            ModuleDescription moduleDescription = getTestModuleDescription();
            Utils.readModule(moduleDescription);
            fail();
        } catch (DesignerException e) {
            assertEquals(IExceptionConstants.INVALID_MODULE_FILE, e.getCode());
            assertThat(e.getParameters()[0]).endsWith("test.Module");
        }
    }

    /**
   * Test for {@link Utils#readModule(String, java.io.InputStream)}, without problems.
   */
    public void test_readModule_InputStream_OK() throws Exception {
        String moduleString = getSourceDQ("<module/>");
        ModuleElement moduleElement = Utils.readModule("my.external.Module", new ByteArrayInputStream(moduleString.getBytes()));
        assertEquals("my.external.Module", moduleElement.getName());
        assertThat(moduleElement.getChildren()).isEmpty();
    }

    /**
   * Test for {@link Utils#readModule(String, java.io.InputStream)}, with problems.
   */
    @DisposeProjectAfter
    public void test_readModule_InputStream_bad() throws Exception {
        String moduleId = "my.external.Module";
        String moduleString = getSourceDQ("<module>");
        InputStream inputStream = new ByteArrayInputStream(moduleString.getBytes());
        try {
            Utils.readModule(moduleId, inputStream);
            fail();
        } catch (DesignerException e) {
            assertEquals(IExceptionConstants.INVALID_MODULE_FILE, e.getCode());
            assertThat(e.getParameters()).contains(moduleId);
        }
    }

    /**
   * Test for {@link Utils#getFilesForResources(IFile, Collection)}. <br>
   * Resource in module "public" folder.
   */
    public void test_getFilesForResources_publicFolder() throws Exception {
        IFile moduleFile = getFileSrc("test/Module.gwt.xml");
        IFile expectedFile = setFileContentSrc("test/public/1.txt", "");
        List<IFile> files = Utils.getFilesForResources(moduleFile, Lists.newArrayList("1.txt"));
        assertThat(files).containsOnly(expectedFile);
    }

    /**
   * Test for {@link Utils#getFilesForResources(IFile, Collection)}. <br>
   * Resource in "war" folder.
   */
    public void test_getFilesForResources_warFolder() throws Exception {
        IFile moduleFile = getFileSrc("test/Module.gwt.xml");
        IFile expectedFile = setFileContent("war/1.txt", "");
        List<IFile> files = Utils.getFilesForResources(moduleFile, Lists.newArrayList("1.txt"));
        assertThat(files).containsOnly(expectedFile);
    }

    /**
   * Test for {@link Utils#getFilesForResources(IFile, Collection)}. <br>
   * Not existing resource.
   */
    public void test_getFilesForResources_noSuchResource() throws Exception {
        IFile moduleFile = getFileSrc("test/Module.gwt.xml");
        List<IFile> files = Utils.getFilesForResources(moduleFile, Lists.newArrayList("NoSuchResource.txt"));
        assertThat(files).isEmpty();
    }

    /**
   * Test for {@link Utils#getFilesForResources(IFile, Collection)}.
   * <p>
   * https://groups.google.com/forum/#!msg/google-web-toolkit/r0Klxfkd7qA/bJiY3p5GG88J
   */
    public void test_getFilesForResources_startsWithSlash_singleSegment() throws Exception {
        IFile moduleFile = getFileSrc("test/Module.gwt.xml");
        List<IFile> files = Utils.getFilesForResources(moduleFile, Lists.newArrayList("/index.html"));
        assertThat(files).isEmpty();
    }

    /**
   * Test for {@link Utils#getFilesForResources(IFile, Collection)}. <br>
   * Resource from required/inherited module in different {@link IProject}.
   */
    @DisposeProjectAfter
    public void test_getFilesForResources_inherited() throws Exception {
        TestProject libProject = new TestProject("libProject");
        try {
            {
                GTestUtils.createModule(libProject, "the.Library");
                setFileContentSrc(libProject.getProject(), "the/public/sub/folder/libResource.txt", "some content");
            }
            m_testProject.addRequiredProject(libProject);
            IFile moduleFile = getFileSrc("test/Module.gwt.xml");
            setFileContent(moduleFile, getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <inherits name='the.Library'/>", "</module>"));
            waitForAutoBuild();
            List<IFile> files = Utils.getFilesForResources(moduleFile, Lists.newArrayList("Module.html", "sub/folder/libResource.txt"));
            assertThat(files).hasSize(2);
            assertResourcePath("war/Module.html", files.get(0));
            assertResourcePath("src/the/public/sub/folder/libResource.txt", files.get(1));
        } finally {
            libProject.dispose();
        }
    }

    /**
   * Test for {@link Utils#getResource(IFile, String)}.
   */
    public void test_getResource_warFolder() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        setFileContent("war/1.txt", "");
        assert_getResource_notNull(module, "1.txt");
    }

    /**
   * Test for {@link Utils#getResource(IFile, String)}.
   * <p>
   * http://fogbugz.instantiations.com/fogbugz/default.php?43760
   */
    @DisposeProjectAfter
    public void test_getResource_mavenFolder_webapp() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        setFileContent("src/main/webapp/1.txt", "");
        assert_getResource_notNull(module, "1.txt");
    }

    /**
   * Test for {@link Utils#getResource(IFile, String)}.
   */
    public void test_getResource_publicFolder() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        setFileContentSrc("test/public/1.txt", "");
        assert_getResource_notNull(module, "test.Module/1.txt");
    }

    /**
   * Test for {@link Utils#getResource(IFile, String)}.
   */
    @DisposeProjectAfter
    public void test_getResource_publicFolder_renameTo() throws Exception {
        {
            IFile moduleFile = getFileSrc("test/Module.gwt.xml");
            setFileContent(moduleFile, "<module rename-to='myModule'/>");
        }
        setFileContentSrc("test/public/1.txt", "");
        waitForAutoBuild();
        ModuleDescription module = getTestModuleDescription();
        assert_getResource_null(module, "test.Module/1.txt");
        assert_getResource_notNull(module, "myModule/1.txt");
        assert_getResource_notNull(module, "1.txt");
    }

    /**
   * Test for {@link Utils#getResource(IFile, String)}.
   */
    public void test_getResource_no() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        assert_getResource_null(module, "test.Module/noSuchResource.txt");
    }

    private static void assert_getResource_notNull(ModuleDescription module, String path) throws Exception {
        InputStream inputStream = Utils.getResource(module, path);
        IOUtils.closeQuietly(inputStream);
        assertNotNull(inputStream);
    }

    private static void assert_getResource_null(ModuleDescription module, String path) throws Exception {
        InputStream inputStream = Utils.getResource(module, path);
        IOUtils.closeQuietly(inputStream);
        assertNull(inputStream);
    }

    /**
   * Test for {@link Utils#isExistingResource(IFile, String)}.
   */
    public void test_isExistingResource_warFolder() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        assertTrue(Utils.isExistingResource(module, "Module.html"));
    }

    /**
   * Test for {@link Utils#isExistingResource(IFile, String)}.
   */
    public void test_isExistingResource_publicFolder() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        setFileContentSrc("test/public/1.txt", "");
        assertTrue(Utils.isExistingResource(module, "test.Module/1.txt"));
    }

    /**
   * Test for {@link Utils#isExistingResource(IFile, String)}.
   */
    public void test_isExistingResource_no() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        assertFalse(Utils.isExistingResource(module, "test.Module/noSuchResource.txt"));
    }

    /**
   * Test for {@link Utils#getHTMLFile(IFile)}.
   */
    public void test_getHTMLFile_warFolder() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        IFile htmlFile = getFile("war/Module.html");
        assertEquals(htmlFile, Utils.getHTMLFile(module));
    }

    /**
   * Test for {@link Utils#getHTMLFile(IFile)}.
   * <p>
   * By default our test web.xml has "welcome-file", but we want to test without it.
   */
    @DisposeProjectAfter
    public void test_getHTMLFile_warFolder_withoutWebXML() throws Exception {
        getFile("war/WEB-INF/web.xml").delete(true, null);
        ModuleDescription module = getTestModuleDescription();
        IFile htmlFile = getFile("war/Module.html");
        assertEquals(htmlFile, Utils.getHTMLFile(module));
    }

    /**
   * Test for {@link Utils#getHTMLFile(IFile)}.
   * <p>
   * Sometimes users try to use empty <code>web.xml</code> file. We should ignore it.
   * <p>
   * http://fogbugz.instantiations.com/fogbugz/default.asp?46031
   */
    @DisposeProjectAfter
    public void test_getHTMLFile_warFolder_emptyWebXML() throws Exception {
        setFileContent("war/WEB-INF/web.xml", "");
        ModuleDescription module = getTestModuleDescription();
        IFile htmlFile = getFile("war/Module.html");
        assertEquals(htmlFile, Utils.getHTMLFile(module));
    }

    /**
   * Test for {@link Utils#getHTMLFile(IFile)}.
   * <p>
   * Sometimes users try to use invalid <code>web.xml</code> file. We should show better error.
   * <p>
   * http://fogbugz.instantiations.com/fogbugz/default.asp?46836
   */
    @DisposeProjectAfter
    public void test_getHTMLFile_warFolder_invalidWebXML() throws Exception {
        setFileContent("war/WEB-INF/web.xml", "invalid content");
        ModuleDescription module = getTestModuleDescription();
        try {
            Utils.getHTMLFile(module);
            fail();
        } catch (Throwable e) {
            DesignerException de = DesignerExceptionUtils.getDesignerException(e);
            assertEquals(IExceptionConstants.INVALID_WEB_XML, de.getCode());
        }
    }

    /**
   * Test for {@link Utils#getHTMLFile(IFile)}.
   */
    @DisposeProjectAfter
    public void test_getHTMLFile_warFolder_useWelcomeFile() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        getFile("war/Module.html").delete(true, null);
        IFile htmlFile = setFileContent("war/EntryPoint.html", "<html/>");
        assertEquals(null, Utils.getHTMLFile(module));
        setFileContent("war/WEB-INF/web.xml", getSource("<web-app>", "  <welcome-file-list>", "    <welcome-file>EntryPoint.html</welcome-file>", "  </welcome-file-list>", "</web-app>"));
        assertEquals(htmlFile, Utils.getHTMLFile(module));
    }

    /**
   * Test for {@link Utils#getHTMLFile(IFile)}.
   * <p>
   * Exception {@link IExceptionConstants#NO_MODULE} should not be masked.
   * <p>
   * http://fogbugz.instantiations.com/fogbugz/default.asp?47616
   */
    @DisposeProjectAfter
    public void test_getHTMLFile_warFolder_useWelcomeFile_whenNoModule() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        setFileContent(getTestModuleFile(), getSource("<!-- filler filler filler filler filler -->", "<module>", "  <inherits name='no.such.Module'/>", "</module>"));
        waitForAutoBuild();
        try {
            Utils.getHTMLFile(module);
        } catch (DesignerException e) {
            assertEquals(IExceptionConstants.NO_MODULE, e.getCode());
        }
    }

    /**
   * Test for {@link Utils#getHTMLFile(IFile)}.
   * <p>
   * http://fogbugz.instantiations.com/fogbugz/default.asp?45214
   */
    @DisposeProjectAfter
    public void test_getHTMLFile_notExistingSourceFolder() throws Exception {
        m_testProject.addSourceFolder("/TestProject/src2");
        ModuleDescription module = getTestModuleDescription();
        IFile htmlFile = Utils.getHTMLFile(module);
        assertResourcePath("war/Module.html", htmlFile);
    }

    /**
   * Test for {@link Utils#getDefaultHTMLName(String)}.
   */
    public void test_getDefaultHTMLName() throws Exception {
        assertEquals("Module.html", Utils.getDefaultHTMLName("test.Module"));
        assertEquals("TheModule.html", Utils.getDefaultHTMLName("my.long.name.for.TheModule"));
    }

    /**
   * Test for {@link Utils#getCssResources(IFile)}.<br>
   * Only default <code>Module.css</code> resource from HTML.
   */
    public void test_getCssResources_fromHTML() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        List<String> resources = Utils.getCssResources(module);
        assertThat(resources).containsOnly("Module.css");
    }

    /**
   * Test for combination {@link Utils#getHTMLFile(IFile)} and {@link Utils#getCssResources(IFile)}.
   */
    @DisposeProjectAfter
    public void test_getCssResources_useWelcomeFile_inSubFolder() throws Exception {
        IFile moduleFile = getTestModuleFile();
        ModuleDescription module = getTestModuleDescription();
        getFile("war/Module.html").delete(true, null);
        IFile htmlFile = setFileContent("war/sub/EntryPoint.html", getSource("<html>", "  <head>", "    <link type='text/css' rel='stylesheet' href='resources/MyStyles.css'/>", "  </head>", "</html>"));
        IFile cssFile = setFileContent("war/sub/resources/MyStyles.css", "");
        assertEquals(null, Utils.getHTMLFile(module));
        setFileContent("war/WEB-INF/web.xml", getSource("<web-app>", "  <welcome-file-list>", "    <welcome-file>sub/EntryPoint.html</welcome-file>", "  </welcome-file-list>", "</web-app>"));
        assertEquals(htmlFile, Utils.getHTMLFile(module));
        {
            List<String> cssResources = Utils.getCssResources(module);
            assertThat(cssResources).containsExactly("sub/resources/MyStyles.css");
            {
                String resource = cssResources.get(0);
                IFile fileForResource = Utils.getFileForResource(moduleFile, resource);
                assertEquals(cssFile, fileForResource);
            }
        }
    }

    /**
   * Test for {@link Utils#getCssResources(IFile)}.<br>
   * Delete default HTML, so no CSS resources at all.
   */
    @DisposeProjectAfter
    public void test_getCssResources_noHTML() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        getFile("war/Module.html").delete(true, null);
        List<String> resources = Utils.getCssResources(module);
        assertThat(resources).isEmpty();
    }

    /**
   * Test for {@link Utils#getCssResources(IFile)}.<br>
   * Use <code>stylesheet</code> element in module file.
   */
    @DisposeProjectAfter
    public void test_getCssResources_fromModule() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        setFileContent(getTestModuleFile(), getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <stylesheet src='fromModule.css'/>", "</module>"));
        waitForAutoBuild();
        List<String> resources = Utils.getCssResources(module);
        assertThat(resources).containsOnly("Module.css", "fromModule.css");
    }

    /**
   * Test for {@link Utils#getCssResources(IFile)}.<br>
   * Use path with leading "/" string.
   */
    @DisposeProjectAfter
    public void test_getCssResources_fromModule_fromRoot() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        setFileContent("war/Module.html", "<html/>");
        setFileContent(getTestModuleFile(), getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <stylesheet src='/css/fromModule.css'/>", "</module>"));
        waitForAutoBuild();
        List<String> resources = Utils.getCssResources(module);
        assertThat(resources).containsOnly("css/fromModule.css");
    }

    /**
   * Test for {@link Utils#getCssResources(IFile)}.<br>
   * Use path with leading "../" string.
   * <p>
   * http://fogbugz.instantiations.com/fogbugz/default.asp?46049
   */
    @DisposeProjectAfter
    public void test_getCssResources_fromModule_forFileInWar() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        setFileContent("war/Module.html", "<html/>");
        setFileContent(getTestModuleFile(), getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <stylesheet src='../css/fromModule.css'/>", "</module>"));
        waitForAutoBuild();
        List<String> resources = Utils.getCssResources(module);
        assertThat(resources).containsOnly("css/fromModule.css");
    }

    /**
   * Test for {@link Utils#getScriptResources(IFile)}.<br>
   * No scripts, because <code>.nocache.js</code> from HTML is ignored.
   */
    public void test_getScriptResources_1() throws Exception {
        assertThat(getFileContent("war/Module.html")).contains(".nocache.js");
        ModuleDescription moduleDescription = getTestModuleDescription();
        List<String> resources = Utils.getScriptResources(moduleDescription);
        assertThat(resources).isEmpty();
    }

    /**
   * Test for {@link Utils#getScriptResources(IFile)}.<br>
   * Add references on new scripts from HTML and module file.
   */
    @DisposeProjectAfter
    public void test_getScriptResources_2() throws Exception {
        IFile moduleFile = getTestModuleFile();
        ModuleDescription moduleDescription = getTestModuleDescription();
        setFileContentSrc("test/public/Module.html", getSourceDQ("<html>", "  <body>", "    <script language='javascript' src='test.Module.nocache.js'></script>", "    <script language='javascript' src='fromHTML.js'></script>", "  </body>", "</html>"));
        setFileContent(moduleFile, getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <script src='fromModule.js'/>", "</module>"));
        waitForAutoBuild();
        List<String> resources = Utils.getScriptResources(moduleDescription);
        assertThat(resources).containsOnly("fromModule.js", "fromHTML.js");
    }

    /**
   * Test for {@link Utils#getScriptResources(IFile)}.
   * <p>
   * We don't want to show Google Maps widget, and even load its script. Script requires key, and
   * users often fail to provide it. In this case script shows warning and locks-up Eclipse.
   * <p>
   * http://fogbugz.instantiations.com/fogbugz/default.php?43650
   */
    @DisposeProjectAfter
    public void test_getScriptResources_ignoreGoogleMaps() throws Exception {
        IFile moduleFile = getTestModuleFile();
        ModuleDescription moduleDescription = getTestModuleDescription();
        setFileContentSrc("test/public/Module.html", "");
        setFileContent(moduleFile, getSource("<module>", "  <script src='http://maps.google.com/maps?gwt=1&amp;file=api&amp;v=2'/>/>", "</module>"));
        waitForAutoBuild();
        List<String> resources = Utils.getScriptResources(moduleDescription);
        assertThat(resources).isEmpty();
    }

    /**
   * Test for {@link Utils#getScriptResources(IFile)}.<br>
   * Use "script" tag without "src" attribute.
   */
    @DisposeProjectAfter
    public void test_getScriptResources_noSrcInScript() throws Exception {
        ModuleDescription moduleDescription = getTestModuleDescription();
        setFileContentSrc("test/public/Module.html", getSourceDQ("<html>", "  <body>", "    <script type='text/javascript'>some script</script>", "  </body>", "</html>"));
        waitForAutoBuild();
        List<String> resources = Utils.getScriptResources(moduleDescription);
        assertThat(resources).isEmpty();
    }

    /**
   * Test for {@link Utils#getDefaultLocale(IFile)}.
   */
    public void test_getDefaultLocale_noOverride() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        String defaultLocale = Utils.getDefaultLocale(module);
        assertEquals("default", defaultLocale);
    }

    /**
   * Test for {@link Utils#getDefaultLocale(IFile)}.
   */
    @DisposeProjectAfter
    public void test_getDefaultLocale_doOverride() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        setFileContent(getTestModuleFile(), getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <inherits name='com.google.gwt.user.User'/>", "  <inherits name='com.google.gwt.i18n.I18N'/>", "  <extend-property name='locale' values='en,es'/>", "  <set-property name='locale' value='en,es'/>", "  <set-property-fallback name='locale' value='en'/>", "</module>"));
        String defaultLocale = Utils.getDefaultLocale(module);
        assertEquals("en", defaultLocale);
    }

    /**
   * Test for {@link Utils#getDefaultLocale(IFile)}.
   */
    @DisposeProjectAfter
    public void test_getDefaultLocale_doOverride_inLibrary() throws Exception {
        {
            IFile libraryModuleFile = GTestUtils.createModule(m_testProject, "the.Library");
            setFileContent(libraryModuleFile, getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <inherits name='com.google.gwt.i18n.I18N'/>", "  <extend-property name='locale' values='en,es'/>", "  <set-property name='locale' value='en,es'/>", "  <set-property-fallback name='locale' value='en'/>", "</module>"));
        }
        ModuleDescription module = getTestModuleDescription();
        setFileContent(getTestModuleFile(), getSourceDQ("<!-- filler filler filler filler filler -->", "<module>", "  <inherits name='com.google.gwt.user.User'/>", "  <inherits name='the.Library'/>", "</module>"));
        String defaultLocale = Utils.getDefaultLocale(module);
        assertEquals("en", defaultLocale);
    }

    @DisposeProjectAfter
    public void test_getDocType_no() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        setFileContentSrc("test/public/Module.html", getSourceDQ("<!-- filler filler filler filler filler -->", "<html>", "  <body>", "    filler filler filler", "  </body>", "</html>"));
        assertEquals("", Utils.getDocType(module));
    }

    @DisposeProjectAfter
    public void test_getDocType_has() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        setFileContentSrc("test/public/Module.html", getSourceDQ("<!doctype html>", "<html>", "  <body>", "    filler filler filler", "  </body>", "</html>"));
        assertEquals("<!doctype html>", Utils.getDocType(module));
    }

    @DisposeProjectAfter
    public void test_getDocType_noHTML() throws Exception {
        ModuleDescription module = getTestModuleDescription();
        getFile("war/Module.html").delete(true, null);
        assertThat(Utils.getDocType(module)).isNull();
    }

    /**
   * Test for {@link Utils#isRemoteService(IResource)}.
   */
    public void test_isRemoteService_IResource() throws Exception {
        {
            IResource resource = getFileSrc("test/Module.gwt.xml");
            assertTrue(resource.exists());
            assertFalse(Utils.isRemoteService(resource));
        }
        {
            IResource resource = getFileSrc("test/client/Module.java");
            assertTrue(resource.exists());
            assertFalse(Utils.isRemoteService(resource));
        }
        {
            IType type = GTestUtils.createTestService(this)[0];
            IResource resource = type.getUnderlyingResource();
            assertThat(resource).isInstanceOf(IFile.class);
            assertTrue(resource.exists());
            assertTrue(Utils.isRemoteService(resource));
        }
    }

    /**
   * Test for {@link Utils#isRemoteService(IJavaElement)}.
   */
    public void test_isRemoteService_IJavaElement() throws Exception {
        IJavaProject javaProject = m_testProject.getJavaProject();
        {
            IJavaElement element = javaProject.findType("test.client.Module");
            assertFalse(Utils.isRemoteService(element));
        }
        {
            IJavaElement element = GTestUtils.createTestService(this)[0];
            assertTrue(Utils.isRemoteService(element));
        }
    }

    /**
   * Test for {@link Utils#isRemoteServiceImpl(IJavaElement)}.
   */
    public void test_isRemoteServiceImpl() throws Exception {
        IJavaProject javaProject = m_testProject.getJavaProject();
        {
            IJavaElement element = javaProject;
            assertFalse(Utils.isRemoteServiceImpl(element));
        }
        {
            IJavaElement element = javaProject.findType("test.client.Module");
            assertFalse(Utils.isRemoteServiceImpl(element));
        }
        {
            IType element = GTestUtils.createTestService(this)[1];
            assertTrue(Utils.isRemoteServiceImpl(element));
            assertTrue(Utils.isRemoteServiceImpl(element.getCompilationUnit()));
        }
    }

    /**
   * Test for {@link Utils#isEntryPoint(IJavaElement)}.
   */
    public void test_isEntryPoint() throws Exception {
        IJavaProject javaProject = m_testProject.getJavaProject();
        {
            IJavaElement element = javaProject;
            assertFalse(Utils.isEntryPoint(element));
        }
        {
            IJavaElement element = javaProject.findType("test.client.Module");
            assertTrue(Utils.isEntryPoint(element));
        }
        {
            IType element = GTestUtils.createTestService(this)[0];
            assertFalse(Utils.isEntryPoint(element));
        }
    }

    /**
   * Test for {@link Utils#getNonNullMonitor(IProgressMonitor)}.
   */
    public void test_getNonNullMonitor() throws Exception {
        {
            IProgressMonitor monitor = EasyMock.createStrictMock(IProgressMonitor.class);
            assertSame(monitor, Utils.getNonNullMonitor(monitor));
        }
        {
            IProgressMonitor monitor = Utils.getNonNullMonitor(null);
            assertNotNull(monitor);
        }
    }

    /**
   * Test for {@link Utils#getProject(String)}.
   */
    public void test_getProject() throws Exception {
        {
            IProject project = Utils.getProject("TestProject");
            assertNotNull(project);
            assertTrue(project.exists());
        }
        {
            IProject project = Utils.getProject("NoSuchProject");
            assertNotNull(project);
            assertFalse(project.exists());
        }
    }

    /**
   * Test for {@link Utils#getJavaProject(String)}.
   */
    public void test_getJavaProject() throws Exception {
        {
            IJavaProject javaProject = Utils.getJavaProject("TestProject");
            assertNotNull(javaProject);
            assertTrue(javaProject.exists());
        }
        {
            IJavaProject javaProject = Utils.getJavaProject("NoSuchProject");
            assertNotNull(javaProject);
            assertFalse(javaProject.exists());
        }
    }

    /**
   * Test for {@link Utils#isGWTProject(IJavaProject)} and {@link Utils#isGWTProject(IProject)}.
   */
    public void test_isGWTProject() throws Exception {
        IJavaProject javaProject = Utils.getJavaProject("TestProject");
        assertTrue(Utils.isGWTProject(javaProject));
        assertTrue(Utils.isGWTProject(javaProject.getProject()));
    }

    /**
   * Test for {@link Utils#isGWTProject(IJavaProject)} and {@link Utils#isGWTProject(IProject)}.
   */
    public void test_isGWTProject_newProject() throws Exception {
        TestProject newProject = new TestProject("newProject");
        try {
            IJavaProject javaProject = newProject.getJavaProject();
            IProject project = javaProject.getProject();
            assertFalse(Utils.isGWTProject(javaProject));
            assertFalse(Utils.isGWTProject(project));
            GTestUtils.configure(newProject);
            assertTrue(Utils.isGWTProject(javaProject));
            assertTrue(Utils.isGWTProject(project));
            ProjectUtils.removeNature(project, Constants.NATURE_ID);
            assertTrue(Utils.isGWTProject(javaProject));
            assertTrue(Utils.isGWTProject(project));
        } finally {
            newProject.dispose();
        }
    }

    /**
   * Test for {@link Utils#isGWTProject(IJavaProject)} and {@link Utils#isGWTProject(IProject)}.
   */
    public void test_isGWTProject_noSuchProject() throws Exception {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("noSuchProject");
        IJavaProject javaProject = JavaCore.create(project);
        assertFalse(project.exists());
        assertFalse(javaProject.exists());
        assertFalse(Utils.isGWTProject(javaProject));
    }

    /**
   * Test for {@link Utils#isGWTProject(IJavaProject)} and {@link Utils#isGWTProject(IProject)}.
   */
    @DisposeProjectAfter
    public void test_isGWTProject_closedProject() throws Exception {
        m_project.close(null);
        waitForAutoBuild();
        assertTrue(m_project.exists());
        assertFalse(m_project.isOpen());
        assertFalse(m_project.isAccessible());
        assertFalse(Utils.isGWTProject(m_javaProject));
        assertFalse(Utils.isGWTProject(m_project));
    }

    /**
   * Test for {@link Utils#isGpeGwtProject(IProject)}.
   */
    public void test_isGpeGwtProject() throws Exception {
        assertFalse(Utils.isGpeGwtProject(m_project));
        ProjectUtils.addNature(m_project, Constants.GPE_NATURE_ID);
        assertTrue(Utils.isGpeGwtProject(m_project));
    }

    /**
   * Test for {@link Utils#getJavaModel()}.
   */
    public void test_getJavaModel() throws Exception {
        IJavaModel javaModel = Utils.getJavaModel();
        assertNotNull(javaModel);
        assertTrue(javaModel.exists());
    }

    /**
   * Test for {@link Utils#getGWTProjects()}.
   */
    public void test_getGWTProjects() throws Exception {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        {
            assertThat(workspaceRoot.getProjects()).hasSize(1);
            List<IJavaProject> gwtProjects = Utils.getGWTProjects();
            assertThat(gwtProjects).hasSize(1);
            assertEquals("TestProject", gwtProjects.get(0).getElementName());
        }
        {
            TestProject newProject = new TestProject("newProject");
            try {
                assertThat(workspaceRoot.getProjects()).hasSize(2);
                {
                    List<IJavaProject> gwtProjects = Utils.getGWTProjects();
                    assertThat(gwtProjects).hasSize(1);
                    assertEquals("TestProject", gwtProjects.get(0).getElementName());
                }
                {
                    GTestUtils.configure(newProject);
                    List<IJavaProject> gwtProjects = Utils.getGWTProjects();
                    assertThat(gwtProjects).hasSize(2);
                    Set<String> gwtProjectNames = ImmutableSet.of(gwtProjects.get(0).getElementName(), gwtProjects.get(1).getElementName());
                    assertThat(gwtProjectNames).containsOnly("TestProject", "newProject");
                }
            } finally {
                newProject.dispose();
            }
        }
    }

    /**
   * Test for {@link Utils#parseUnit(ICompilationUnit)}.
   */
    public void test_parseUnit() throws Exception {
        IType type = m_testProject.getJavaProject().findType("test.client.Module");
        CompilationUnit compilationUnit = Utils.parseUnit(type.getCompilationUnit());
        assertNotNull(compilationUnit);
        assertEquals("Module", DomGenerics.types(compilationUnit).get(0).getName().getIdentifier());
    }

    /**
   * Asserts that {@link ModuleDescription} if {@link IFile} based and has expected path relative to
   * its project.
   */
    private static void assertModuleDescriptionPath(String expectedPath, ModuleDescription module) {
        assertNotNull("No module.", module);
        IResource resource = ((DefaultModuleDescription) module).getFile();
        assertResourcePath(expectedPath, resource);
    }

    /**
   * Asserts that {@link IResource} has expected path relative to its project.
   */
    private static void assertResourcePath(String expectedPath, IResource resource) {
        assertNotNull("No resource.", resource);
        String actualPath = resource.getProjectRelativePath().toPortableString();
        assertEquals(expectedPath, actualPath);
    }

    /**
   * @return the {@link IFile} of standard test module.
   */
    private static IFile getTestModuleFile() throws Exception {
        return getFileSrc("test/Module.gwt.xml");
    }

    /**
   * @return the {@link ModuleDescription} of standard test module.
   */
    private static ModuleDescription getTestModuleDescription() throws Exception {
        IFile moduleFile = getTestModuleFile();
        return Utils.getExactModule(moduleFile);
    }
}
