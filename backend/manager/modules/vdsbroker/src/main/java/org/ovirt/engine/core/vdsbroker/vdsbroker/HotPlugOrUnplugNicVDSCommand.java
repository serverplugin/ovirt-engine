package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.common.vdscommands.VmNicDeviceVDSParameters;
import org.ovirt.engine.core.utils.StringMapUtils;
import org.ovirt.engine.core.vdsbroker.builder.vminfo.VmInfoBuildUtils;

public abstract class HotPlugOrUnplugNicVDSCommand<P  extends VmNicDeviceVDSParameters> extends VdsBrokerCommand<P> {
    @Inject
    private VmInfoBuildUtils vmInfoBuildUtils;

    public HotPlugOrUnplugNicVDSCommand(P parameters) {
        super(parameters);
    }

    protected Map<String, Object> createParametersStruct() {
        Map<String, Object> struct = new HashMap<>();
        struct.put(VdsProperties.vm_guid, getParameters().getVm().getId().toString());
        if (FeatureSupported.isDomainXMLSupported(getParameters().getVm().getClusterCompatibilityVersion())) {
            try {
                struct.put(VdsProperties.engineXml, generateDomainXml());
            } catch (JAXBException e) {
                log.error("failed to create xml for hot-(un)plug", e);
                throw new RuntimeException(e);
            }
        } else {
            struct.put(VdsProperties.VM_NETWORK_INTERFACE, initNicStructure());
        }
        return struct;
    }

    private Map<String, Object> initNicStructure() {
        Map<String, Object> map = new HashMap<>();
        VmNic nic = getParameters().getNic();
        VmDevice vmDevice = getParameters().getVmDevice();
        VM vm = getParameters().getVm();

        if (!nic.isPassthrough()) {
            map.put(VdsProperties.Type, vmDevice.getType().getValue());
            map.put(VdsProperties.Device, VmDeviceType.BRIDGE.getName());
            map.put(VdsProperties.MAC_ADDR, nic.getMacAddress());
            map.put(VdsProperties.LINK_ACTIVE, String.valueOf(nic.isLinked()));

            if (StringUtils.isNotBlank(vmDevice.getAddress())) {
                map.put(VdsProperties.Address, StringMapUtils.string2Map(getParameters().getVmDevice().getAddress()));
            }

            map.put(VdsProperties.SpecParams, vmDevice.getSpecParams());
            map.put(VdsProperties.NIC_TYPE,
                    vmInfoBuildUtils.evaluateInterfaceType(VmInterfaceType.forValue(nic.getType()),
                            vm.getHasAgent()));
            map.put(VdsProperties.DeviceId, vmDevice.getId().getDeviceId().toString());

            vmInfoBuildUtils.addProfileDataToNic(map, vm, vmDevice, nic);
            vmInfoBuildUtils.addNetworkFiltersToNic(map, nic);
        } else {
            vmInfoBuildUtils.addNetworkVirtualFunctionProperties(map, nic, vmDevice, vmDevice.getHostDevice(), vm);
        }
        return map;
    }

    protected abstract String generateDomainXml() throws JAXBException;
}
