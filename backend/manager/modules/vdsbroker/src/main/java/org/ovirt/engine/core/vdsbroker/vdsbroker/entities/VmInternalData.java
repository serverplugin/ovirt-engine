package org.ovirt.engine.core.vdsbroker.vdsbroker.entities;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmGuestAgentInterface;
import org.ovirt.engine.core.common.businessentities.VmJob;
import org.ovirt.engine.core.common.businessentities.VmStatistics;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.storage.LUNs;

/**
 * This class represents the internal data of a VM, including {@link VmDynamic}, {@link VmStatistics} and
 * {@link VmGuestAgentInterface}, to manage data received from the VM's host.
 */
public class VmInternalData {

    private VmDynamic vmDynamic;
    private VmStatistics vmStatistics;
    private List<VmGuestAgentInterface> vmGuestAgentInterfaces;
    /** Timestamp on the dynamic data we get from VDSM */
    private Double timestamp;

    // A map represents VM's LUN disks (LUN ID -> LUNs object)
    private Map<String, LUNs> lunsMap;
    private List<VmJob> vmJobs;
    private List<VmNetworkInterface> interfaceStatistics;

    public VmInternalData(VmDynamic vmDynamic, Double timestamp) {
        this(vmDynamic, null, timestamp);
    }

    public VmInternalData(VmDynamic vmDynamic, VmStatistics vmStatistics, Double timestamp) {
        this(vmDynamic, vmStatistics, null, null, null,
                Collections.<String, LUNs>emptyMap(), timestamp);
    }

    public VmInternalData(VmDynamic vmDynamic,
            VmStatistics vmStatistics,
            List<VmJob> vmJobs,
            List<VmNetworkInterface> interfaceStatistics,
            List<VmGuestAgentInterface> vmGuestAgentInterfaces,
            Map<String, LUNs> lunsMap,
            Double timestamp) {
        this.vmDynamic = vmDynamic;
        this.vmStatistics = vmStatistics;
        this.vmGuestAgentInterfaces = vmGuestAgentInterfaces;
        this.lunsMap = lunsMap;
        this.timestamp = timestamp;
        this.vmJobs = vmJobs;
        this.interfaceStatistics = interfaceStatistics;
    }

    public VmDynamic getVmDynamic() {
        return vmDynamic;
    }

    public void setVmDynamic(VmDynamic vmDynamic) {
        this.vmDynamic = vmDynamic;
    }

    public VmStatistics getVmStatistics() {
        return vmStatistics;
    }

    public void setVmStatistics(VmStatistics vmStatistics) {
        this.vmStatistics = vmStatistics;
    }

    public List<VmGuestAgentInterface> getVmGuestAgentInterfaces() {
        return vmGuestAgentInterfaces;
    }

    public void setVmGuestAgentInterfaces(List<VmGuestAgentInterface> vmGuestAgentInterfaces) {
        this.vmGuestAgentInterfaces = vmGuestAgentInterfaces;
    }

    public Map<String, LUNs> getLunsMap() {
        return lunsMap;
    }

    public void setLunsMap(Map<String, LUNs> lunsMap) {
        this.lunsMap = lunsMap;
    }

    public List<VmJob> getVmJobs() {
        return vmJobs;
    }

    public void setVmJobs(List<VmJob> vmJobs) {
        this.vmJobs = vmJobs;
    }

    public List<VmNetworkInterface> getInterfaceStatistics() {
        return this.interfaceStatistics;
    }

    public void setInterfaceStatistics(List<VmNetworkInterface> interfaceStatistics) {
        this.interfaceStatistics = interfaceStatistics;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                vmDynamic,
                vmGuestAgentInterfaces,
                vmStatistics,
                lunsMap,
                interfaceStatistics
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VmInternalData)) {
            return false;
        }
        VmInternalData other = (VmInternalData) obj;
        return Objects.equals(vmDynamic, other.vmDynamic)
                && Objects.equals(vmGuestAgentInterfaces, other.vmGuestAgentInterfaces)
                && Objects.equals(vmStatistics, other.vmStatistics)
                && Objects.equals(lunsMap, other.lunsMap)
                && Objects.equals(interfaceStatistics, other.interfaceStatistics);
    }

    public Double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Double timestamp) {
        this.timestamp = timestamp;
    }
}
