package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.common.utils.MockConfigRule.mockConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.bll.network.macpool.MacPoolPerCluster;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainValidator;
import org.ovirt.engine.core.common.action.AddVmFromSnapshotParameters;
import org.ovirt.engine.core.common.action.AddVmParameters;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.OsType;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.MockConfigRule;
import org.ovirt.engine.core.common.utils.SimpleDependencyInjector;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.ClusterDao;
import org.ovirt.engine.core.dao.DiskDao;
import org.ovirt.engine.core.dao.QuotaDao;
import org.ovirt.engine.core.dao.SnapshotDao;
import org.ovirt.engine.core.dao.StorageDomainDao;
import org.ovirt.engine.core.dao.VmDao;
import org.ovirt.engine.core.dao.VmDeviceDao;
import org.ovirt.engine.core.dao.VmStaticDao;
import org.ovirt.engine.core.dao.VmTemplateDao;

@SuppressWarnings("serial")
public class AddVmCommandTest extends BaseCommandTest {

    private static final Guid STORAGE_DOMAIN_ID_1 = Guid.newGuid();
    private static final Guid STORAGE_DOMAIN_ID_2 = Guid.newGuid();
    protected static final int TOTAL_NUM_DOMAINS = 2;
    private static final int NUM_DISKS_STORAGE_DOMAIN_1 = 3;
    private static final int NUM_DISKS_STORAGE_DOMAIN_2 = 3;
    private static final int REQUIRED_DISK_SIZE_GB = 10;
    protected static final int AVAILABLE_SPACE_GB = 11;
    private static final int USED_SPACE_GB = 4;
    private static final Guid STORAGE_POOL_ID = Guid.newGuid();
    protected static final String CPU_ID = "0";
    private VmTemplate vmTemplate;
    protected Cluster cluster;
    private StoragePool storagePool;
    protected StorageDomainValidator storageDomainValidator;

    private static final Map<String, String> migrationMap = new HashMap<>();

    static {
        migrationMap.put("undefined", "true");
        migrationMap.put("x86_64", "true");
        migrationMap.put("ppc64", "false");
    }

    @Rule
    public MockConfigRule mcr = new MockConfigRule(
            mockConfig(ConfigValues.ValidNumOfMonitors, Arrays.asList("1", "2", "4")),
            mockConfig(ConfigValues.IsMigrationSupported, Version.v4_0, migrationMap),
            mockConfig(ConfigValues.MaxIoThreadsPerVm, 127)
    );

    @Rule
    public InjectorRule injectorRule = new InjectorRule();

    @Mock
    MacPoolPerCluster macPoolPerCluster;

    @Mock
    StorageDomainDao sdDao;

    @Mock
    VmTemplateDao vmTemplateDao;

    @Mock
    VmDao vmDao;

    @Mock
    VmStaticDao vmStaticDao;

    @Mock
    ClusterDao clusterDao;

    @Mock
    BackendInternal backend;

    @Mock
    VDSBrokerFrontend vdsBrokerFrontend;

    @Mock
    SnapshotDao snapshotDao;

    @Mock
    CpuFlagsManagerHandler cpuFlagsManagerHandler;

    @Mock
    OsRepository osRepository;

    @Mock
    VmDeviceDao deviceDao;

    @Mock
    private VmDeviceDao vmDeviceDao;

    @Mock
    private DiskDao diskDao;

    @Mock
    private QuotaDao quotaDao;

    @InjectMocks
    private VmDeviceUtils vmDeviceUtils;

    @Before
    public void initTest() {
        mockCpuFlagsManagerHandler();
        mockOsRepository();

        injectorRule.bind(VmDeviceUtils.class, vmDeviceUtils);
        VmHandler.init();
    }

    protected void mockCpuFlagsManagerHandler() {
        injectorRule.bind(CpuFlagsManagerHandler.class, cpuFlagsManagerHandler);
        when(cpuFlagsManagerHandler.getCpuId(anyString(), any(Version.class))).thenReturn(CPU_ID);
    }

    protected void mockOsRepository() {
        SimpleDependencyInjector.getInstance().bind(OsRepository.class, osRepository);
        VmHandler.init();
        when(osRepository.isWindows(0)).thenReturn(true);
        when(osRepository.isCpuSupported(anyInt(), any(Version.class), anyString())).thenReturn(true);
    }

    @Test
    public void canAddVm() {
        List<String> reasons = new ArrayList<>();
        final int domainSizeGB = 20;
        AddVmCommand<AddVmParameters> cmd = setupCanAddVmTests(domainSizeGB);
        cmd.init();
        doReturn(true).when(cmd).validateCustomProperties(any(VmStatic.class), anyListOf(String.class));
        doReturn(true).when(cmd).validateSpaceRequirements();
        assertTrue("vm could not be added", cmd.canAddVm(reasons, Collections.singletonList(createStorageDomain(domainSizeGB))));
    }

    @Test
    public void isVirtioScsiEnabledDefaultedToTrue() {
        AddVmCommand<AddVmParameters> cmd = setupCanAddVmTests(0);
        Cluster cluster = createCluster();
        doReturn(cluster).when(cmd).getCluster();
        cmd.getParameters().getVm().setClusterId(cluster.getId());
        cmd.initEffectiveCompatibilityVersion();
        when(osRepository.getDiskInterfaces(any(Integer.class), any(Version.class))).thenReturn(
                new ArrayList<>(Collections.singletonList("VirtIO_SCSI")));
        assertTrue("isVirtioScsiEnabled hasn't been defaulted to true on cluster >= 3.3.", cmd.isVirtioScsiEnabled());
    }

    @Test
    public void validateSpaceAndThreshold() {
        AddVmCommand<AddVmParameters> command = setupCanAddVmTests(0);
        doReturn(ValidationResult.VALID).when(storageDomainValidator).isDomainWithinThresholds();
        doReturn(ValidationResult.VALID).when(storageDomainValidator).hasSpaceForNewDisks(anyListOf(DiskImage.class));
        doReturn(storageDomainValidator).when(command).createStorageDomainValidator(any(StorageDomain.class));
        assertTrue(command.validateSpaceRequirements());
        verify(storageDomainValidator, times(TOTAL_NUM_DOMAINS)).hasSpaceForNewDisks(anyListOf(DiskImage.class));
        verify(storageDomainValidator, never()).hasSpaceForClonedDisks(anyListOf(DiskImage.class));
    }

    @Test
    public void validateSpaceNotEnough() throws Exception {
        AddVmCommand<AddVmParameters> command = setupCanAddVmTests(0);
        doReturn(ValidationResult.VALID).when(storageDomainValidator).isDomainWithinThresholds();
        doReturn(new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_STORAGE_DOMAIN)).
                when(storageDomainValidator).hasSpaceForNewDisks(anyListOf(DiskImage.class));
        doReturn(storageDomainValidator).when(command).createStorageDomainValidator(any(StorageDomain.class));
        assertFalse(command.validateSpaceRequirements());
        verify(storageDomainValidator).hasSpaceForNewDisks(anyListOf(DiskImage.class));
        verify(storageDomainValidator, never()).hasSpaceForClonedDisks(anyListOf(DiskImage.class));
    }

    @Test
    public void validateSpaceNotWithinThreshold() throws Exception {
        AddVmCommand<AddVmParameters> command = setupCanAddVmTests(0);
        doReturn(new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_STORAGE_DOMAIN)).
               when(storageDomainValidator).isDomainWithinThresholds();
        doReturn(storageDomainValidator).when(command).createStorageDomainValidator(any(StorageDomain.class));
        assertFalse(command.validateSpaceRequirements());
    }

    protected void mockNonInterestingMethodsForCloneVmFromSnapshot(AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd) {
        mockUninterestingMethods(cmd);
        doReturn(true).when(cmd).checkCpuSockets();
        doReturn(null).when(cmd).getVmFromConfiguration();
    }

    private AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> createVmFromSnapshotCommand(VM vm,
            Guid sourceSnapshotId) {
        AddVmFromSnapshotParameters param = new AddVmFromSnapshotParameters();
        param.setVm(vm);
        param.setSourceSnapshotId(sourceSnapshotId);
        param.setStorageDomainId(Guid.newGuid());
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd = new AddVmFromSnapshotCommand<>(param, null);
        cmd = spy(cmd);
        doReturn(vm).when(cmd).getVm();
        doReturn(createVmTemplate()).when(cmd).getVmTemplate();
        mockDaos(cmd);
        doReturn(snapshotDao).when(cmd).getSnapshotDao();
        mockBackend(cmd);
        return cmd;
    }

    protected AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> setupCanAddVmFromSnapshotTests
            (final int domainSizeGB, Guid sourceSnapshotId) {
        VM vm = initializeMock(domainSizeGB);
        initializeVmDaoMock(vm);
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd = createVmFromSnapshotCommand(vm, sourceSnapshotId);
        initCommandMethods(cmd);
        return cmd;
    }

    private void initializeVmDaoMock(VM vm) {
        when(vmDao.get(any(Guid.class))).thenReturn(vm);
    }

    private void initializeVmStaticDaoMock(VM vm) {
        when(vmStaticDao.get(any(Guid.class))).thenReturn(vm.getStaticData());
    }

    private AddVmCommand<AddVmParameters> setupCanAddVmTests(final int domainSizeGB) {
        VM vm = initializeMock(domainSizeGB);
        AddVmCommand<AddVmParameters> cmd = createCommand(vm);
        cmd.macPoolPerCluster = macPoolPerCluster;
        initCommandMethods(cmd);
        initializeVmStaticDaoMock(vm);
        doReturn(createVmTemplate()).when(cmd).getVmTemplate();
        doReturn(createStoragePool()).when(cmd).getStoragePool();
        return cmd;
    }

    protected static <T extends AddVmParameters> void initCommandMethods(AddVmCommand<T> cmd) {
        doReturn(Guid.newGuid()).when(cmd).getStoragePoolId();
        doReturn(true).when(cmd).canAddVm(anyListOf(String.class), anyString(), any(Guid.class), anyInt());
        doReturn(STORAGE_POOL_ID).when(cmd).getStoragePoolId();
    }

    private VM initializeMock(final int domainSizeGB) {
        mockVmTemplateDaoReturnVmTemplate();
        mockStorageDomainDaoGetForStoragePool(domainSizeGB);
        mockStorageDomainDaoGet(domainSizeGB);
        return createVm();
    }

    protected void mockBackend(AddVmCommand<?> cmd) {
        doReturn(backend).when(cmd).getBackend();
    }

    protected void mockDaos(AddVmCommand<?> cmd) {
        doReturn(vmDao).when(cmd).getVmDao();
        doReturn(sdDao).when(cmd).getStorageDomainDao();
        doReturn(vmTemplateDao).when(cmd).getVmTemplateDao();
        doReturn(clusterDao).when(cmd).getClusterDao();
        doReturn(deviceDao).when(cmd).getVmDeviceDao();
        doReturn(vmStaticDao).when(cmd).getVmStaticDao();
    }

    protected void mockStorageDomainDaoGetForStoragePool(int domainSpaceGB) {
        when(sdDao.getForStoragePool(any(Guid.class), any(Guid.class))).thenReturn(createStorageDomain(domainSpaceGB));
    }

    private void mockStorageDomainDaoGet(final int domainSpaceGB) {
        doAnswer(invocation -> {
            StorageDomain result = createStorageDomain(domainSpaceGB);
            result.setId((Guid) invocation.getArguments()[0]);
            return result;
        }).when(sdDao).get(any(Guid.class));
    }

    protected void mockVmTemplateDaoReturnVmTemplate() {
        when(vmTemplateDao.get(any(Guid.class))).thenReturn(createVmTemplate());
    }

    protected VmTemplate createVmTemplate() {
        if (vmTemplate == null) {
            vmTemplate = new VmTemplate();
            vmTemplate.setStoragePoolId(STORAGE_POOL_ID);
            DiskImage image = createDiskImageTemplate();
            vmTemplate.getDiskTemplateMap().put(image.getImageId(), image);
            HashMap<Guid, DiskImage> diskImageMap = new HashMap<>();
            DiskImage diskImage = createDiskImage(REQUIRED_DISK_SIZE_GB);
            diskImageMap.put(diskImage.getId(), diskImage);
            vmTemplate.setDiskImageMap(diskImageMap);
        }
        return vmTemplate;
    }

    protected Cluster createCluster() {
        if (cluster == null) {
            cluster = new Cluster();
            cluster.setClusterId(Guid.newGuid());
            cluster.setCompatibilityVersion(Version.v4_0);
            cluster.setCpuName("Intel Conroe Family");
            cluster.setArchitecture(ArchitectureType.x86_64);
        }

        return cluster;
    }

    private Cluster createPpcCluster() {
        if (cluster == null) {
            cluster = new Cluster();
            cluster.setClusterId(Guid.newGuid());
            cluster.setCompatibilityVersion(Version.v4_0);
            cluster.setCpuName("PPC8");
            cluster.setArchitecture(ArchitectureType.ppc64);
        }

        return cluster;
    }

    protected StoragePool createStoragePool() {
        if (storagePool == null) {
            storagePool = new StoragePool();
            storagePool.setId(STORAGE_POOL_ID);
            storagePool.setStatus(StoragePoolStatus.Up);
        }
        return storagePool;
    }

    private static DiskImage createDiskImageTemplate() {
        DiskImage i = new DiskImage();
        i.setSizeInGigabytes(USED_SPACE_GB + AVAILABLE_SPACE_GB);
        i.setActualSizeInBytes(REQUIRED_DISK_SIZE_GB * 1024L * 1024L * 1024L);
        i.setImageId(Guid.newGuid());
        i.setStorageIds(new ArrayList<>(Collections.singletonList(STORAGE_DOMAIN_ID_1)));
        return i;
    }

    private static DiskImage createDiskImage(int size) {
        DiskImage diskImage = new DiskImage();
        diskImage.setSizeInGigabytes(size);
        diskImage.setActualSize(size);
        diskImage.setId(Guid.newGuid());
        diskImage.setImageId(Guid.newGuid());
        diskImage.setStorageIds(new ArrayList<>(Collections.singletonList(STORAGE_DOMAIN_ID_1)));
        return diskImage;
    }

    protected StorageDomain createStorageDomain(int availableSpace) {
        StorageDomain sd = new StorageDomain();
        sd.setStorageDomainType(StorageDomainType.Master);
        sd.setStatus(StorageDomainStatus.Active);
        sd.setAvailableDiskSize(availableSpace);
        sd.setUsedDiskSize(USED_SPACE_GB);
        sd.setId(STORAGE_DOMAIN_ID_1);
        return sd;
    }

    protected static VM createVm() {
        VM vm = new VM();
        VmDynamic dynamic = new VmDynamic();
        VmStatic stat = new VmStatic();
        stat.setVmtGuid(Guid.newGuid());
        stat.setName("testVm");
        stat.setPriority(1);
        vm.setStaticData(stat);
        vm.setDynamicData(dynamic);
        vm.setSingleQxlPci(false);
        return vm;
    }

    private AddVmCommand<AddVmParameters> createCommand(VM vm) {
        AddVmParameters param = new AddVmParameters(vm);
        AddVmCommand<AddVmParameters> cmd = new AddVmCommand<>(param, null);
        cmd = spy(cmd);
        mockDaos(cmd);
        mockBackend(cmd);
        mockVmDeviceUtils(cmd);
        doReturn(new Cluster()).when(cmd).getCluster();
        doReturn(createVmTemplate()).when(cmd).getVmTemplate();
        doNothing().when(cmd).initTemplateDisks();
        generateStorageToDisksMap(cmd);
        initDestSDs(cmd);
        storageDomainValidator = mock(StorageDomainValidator.class);
        doReturn(ValidationResult.VALID).when(storageDomainValidator).isDomainWithinThresholds();
        doReturn(storageDomainValidator).when(cmd).createStorageDomainValidator(any(StorageDomain.class));
        return cmd;
    }

    protected void mockVmDeviceUtils(AddVmCommand<AddVmParameters> cmd) {
        doReturn(vmDeviceUtils).when(cmd).getVmDeviceUtils();
    }

    protected void generateStorageToDisksMap(AddVmCommand<? extends AddVmParameters> command) {
        command.storageToDisksMap = new HashMap<>();
        command.storageToDisksMap.put(STORAGE_DOMAIN_ID_1, generateDisksList(NUM_DISKS_STORAGE_DOMAIN_1));
        command.storageToDisksMap.put(STORAGE_DOMAIN_ID_2, generateDisksList(NUM_DISKS_STORAGE_DOMAIN_2));
    }

    private static List<DiskImage> generateDisksList(int size) {
        List<DiskImage> disksList = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            DiskImage diskImage = createDiskImage(REQUIRED_DISK_SIZE_GB);
            disksList.add(diskImage);
        }
        return disksList;
    }

    protected void initDestSDs(AddVmCommand<? extends AddVmParameters> command) {
        StorageDomain sd1 = new StorageDomain();
        StorageDomain sd2 = new StorageDomain();
        sd1.setId(STORAGE_DOMAIN_ID_1);
        sd2.setId(STORAGE_DOMAIN_ID_2);
        command.destStorages.put(STORAGE_DOMAIN_ID_1, sd1);
        command.destStorages.put(STORAGE_DOMAIN_ID_2, sd2);
    }

    protected List<DiskImage> createDiskSnapshot(Guid diskId, int numOfImages) {
        List<DiskImage> disksList = new ArrayList<>();
        for (int i = 0; i < numOfImages; ++i) {
            DiskImage diskImage = new DiskImage();
            diskImage.setActive(false);
            diskImage.setId(diskId);
            diskImage.setImageId(Guid.newGuid());
            diskImage.setParentId(Guid.newGuid());
            diskImage.setImageStatus(ImageStatus.OK);
            disksList.add(diskImage);
        }
        return disksList;
    }

    protected<T extends AddVmParameters> void mockUninterestingMethods(AddVmCommand<T> spy) {
        doReturn(true).when(spy).isVmNameValidLength(any(VM.class));
        doReturn(false).when(spy).isVmWithSameNameExists(anyString(), any(Guid.class));
        doReturn(STORAGE_POOL_ID).when(spy).getStoragePoolId();
        doReturn(createVmTemplate()).when(spy).getVmTemplate();
        doReturn(createCluster()).when(spy).getCluster();
        doReturn(true).when(spy).areParametersLegal(anyListOf(String.class));
        doReturn(Collections.emptyList()).when(spy).getVmInterfaces();
        doReturn(Collections.emptyList()).when(spy).getDiskVmElements();
        doReturn(false).when(spy).isVirtioScsiControllerAttached(any(Guid.class));
        doReturn(true).when(osRepository).isSoundDeviceEnabled(any(Integer.class), any(Version.class));
        spy.setVmTemplateId(Guid.newGuid());
    }

    @Test
    public void testBeanValidations() {
        assertTrue(createCommand(initializeMock(1)).validateInputs());
    }

    @Test
    public void testPatternBasedNameFails() {
        AddVmCommand<AddVmParameters> cmd = createCommand(initializeMock(1));
        cmd.getParameters().getVm().setName("aa-??bb");
        assertFalse("Pattern-based name should not be supported for VM", cmd.validateInputs());
    }

    @Test
    public void refuseBalloonOnPPC() {
        AddVmCommand<AddVmParameters> cmd = setupCanAddPpcTest();
        cmd.getParameters().setBalloonEnabled(true);
        when(osRepository.isBalloonEnabled(cmd.getParameters().getVm().getVmOsId(), cmd.getCluster().getCompatibilityVersion())).thenReturn(false);

        ValidateTestUtils.runAndAssertValidateFailure(cmd, EngineMessage.BALLOON_REQUESTED_ON_NOT_SUPPORTED_ARCH);
    }

    @Test
    public void refuseSoundDeviceOnPPC() {
        AddVmCommand<AddVmParameters> cmd = setupCanAddPpcTest();
        cmd.getParameters().setSoundDeviceEnabled(true);
        when(osRepository.isSoundDeviceEnabled(cmd.getParameters().getVm().getVmOsId(), cmd.getCluster().getCompatibilityVersion())).thenReturn(false);

        ValidateTestUtils.runAndAssertValidateFailure
                (cmd, EngineMessage.SOUND_DEVICE_REQUESTED_ON_NOT_SUPPORTED_ARCH);
    }

    private AddVmCommand<AddVmParameters> setupCanAddPpcTest() {
        final int domainSizeGB = 20;
        AddVmCommand<AddVmParameters> cmd = setupCanAddVmTests(domainSizeGB);

        doReturn(true).when(cmd).validateSpaceRequirements();
        doReturn(true).when(cmd).buildAndCheckDestStorageDomains();
        cmd.getParameters().getVm().setClusterArch(ArchitectureType.ppc64);
        Cluster cluster = new Cluster();
        cluster.setArchitecture(ArchitectureType.ppc64);
        cluster.setCompatibilityVersion(Version.getLast());
        doReturn(cluster).when(cmd).getCluster();

        return cmd;
    }

    @Test
    public void testStoragePoolDoesntExist() {
        final int domainSizeGB = 20;
        AddVmCommand<AddVmParameters> cmd = setupCanAddVmTests(domainSizeGB);

        doReturn(null).when(cmd).getStoragePool();

        ValidateTestUtils.runAndAssertValidateFailure
                (cmd, EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_NOT_EXIST);
    }

    @Test
    public void testBlockUseHostCpuWithPPCArch() {
        AddVmCommand<AddVmParameters> cmd = setupCanAddPpcTest();
        cmd.setEffectiveCompatibilityVersion(Version.v4_0);
        doReturn(Collections.emptyList()).when(cmd).getImagesToCheckDestinationStorageDomains();
        Cluster cluster = createPpcCluster();
        when(clusterDao.get(any(Guid.class))).thenReturn(cluster);
        doReturn(true).when(cmd).areParametersLegal(Collections.emptyList());
        doReturn(true).when(cmd).validateAddVmCommand();
        doReturn(true).when(cmd).isVmNameValidLength(any(VM.class));
        when(osRepository.getArchitectureFromOS(any(Integer.class))).thenReturn(ArchitectureType.ppc64);
        cmd.getParameters().getVm().setClusterArch(ArchitectureType.ppc64);
        cmd.getParameters().getVm().setUseHostCpuFlags(true);
        cmd.getParameters().getVm().setMigrationSupport(MigrationSupport.PINNED_TO_HOST);
        cmd.getParameters().getVm().setClusterId(cluster.getId());
        cmd.getParameters().getVm().setVmOs(OsType.Other.ordinal());

        ValidateTestUtils.runAndAssertValidateFailure
                (cmd, EngineMessage.USE_HOST_CPU_REQUESTED_ON_UNSUPPORTED_ARCH);
    }

    @Test
    public void testValidQuota() {
        Guid quotaId = Guid.newGuid();

        Quota quota = new Quota();
        quota.setId(quotaId);

        AddVmCommand<AddVmParameters> cmd = setupCanAddVmTests(10);

        when(quotaDao.getById(quotaId)).thenReturn(quota);
        doReturn(quotaDao).when(cmd).getQuotaDao();

        StoragePool storagePool = createStoragePool();
        quota.setStoragePoolId(storagePool.getId());
        cmd.setStoragePool(storagePool);

        cmd.getParameters().getVm().setQuotaId(quotaId);

        assertTrue(cmd.validateQuota(quotaId));
        assertTrue(cmd.getReturnValue().getValidationMessages().isEmpty());
    }

    @Test
    public void testNonExistingQuota() {
        AddVmCommand<AddVmParameters> cmd = setupCanAddVmTests(10);

        doReturn(quotaDao).when(cmd).getQuotaDao();

        Guid quotaId = Guid.newGuid();
        cmd.getParameters().getVm().setQuotaId(quotaId);

        assertFalse(cmd.validateQuota(quotaId));
        ValidateTestUtils.assertValidationMessages("", cmd, EngineMessage.ACTION_TYPE_FAILED_QUOTA_NOT_EXIST);
    }
}
