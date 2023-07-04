package org.jsynthlib.synthdrivers.YamahaDX7;

import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyVoiceSingleDriver;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;

public class YamahaDX7VoiceSingleDriver extends DX7FamilyVoiceSingleDriver {

    public YamahaDX7VoiceSingleDriver(final Device device) {
        super(device, YamahaDX7VoiceConstants.INIT_VOICE, YamahaDX7VoiceConstants.SINGLE_VOICE_PATCH_NUMBERS, YamahaDX7VoiceConstants.SINGLE_VOICE_BANK_NUMBERS);
    }

    public void sendPatch(Patch p) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7SysexHelper.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) (0x21), (byte) (0x25));
        }
        if ((((DX7FamilyDevice) (getDevice())).getSPBPflag() & 0x01) == 1) {
            YamahaDX7SysexHelper.mkSysInfoAvail(this, (byte) (getChannel() + 0x10));
        }
        sendPatchWorker(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7SysexHelper.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) (bankNum + 0x21), (byte) (bankNum + 0x25));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.MEMORY_PROTECTION_STRING);
            }
        }
        if ((((DX7FamilyDevice) (getDevice())).getSPBPflag() & 0x01) == 1) {
            YamahaDX7SysexHelper.mkSysInfoAvail(this, (byte) (getChannel() + 0x10));
            sendPatchWorker(p);
            YamahaDX7SysexHelper.chBank(this, (byte) (getChannel() + 0x10), (byte) (bankNum + 0x25));
            send(YamahaDX7SysexHelper.depressStore.toSysexMessage(getChannel() + 0x10));
            YamahaDX7SysexHelper.chPatch(this, (byte) (getChannel() + 0x10), (byte) (patchNum));
            send(YamahaDX7SysexHelper.releaseStore.toSysexMessage(getChannel() + 0x10));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.RECEIVE_STRING);
            }
            sendPatchWorker(p);
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.STORE_SINGLE_VOICE_STRING);
            }
        }
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSPBPflag() & 0x01) == 1) {
            YamahaDX7SysexHelper.mkSysInfoAvail(this, (byte) (getChannel() + 0x10));
            YamahaDX7SysexHelper.chBank(this, (byte) (getChannel() + 0x10), (byte) (bankNum + 0x25));
            YamahaDX7SysexHelper.chPatch(this, (byte) (getChannel() + 0x10), (byte) (patchNum));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.REQUEST_VOICE_STRING);
            }
        }
    }

    public JSLFrame editPatch(Patch p) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7SysexHelper.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) (0x21), (byte) (0x25));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.MEMORY_PROTECTION_STRING);
            }
        }
        if ((((DX7FamilyDevice) (getDevice())).getSPBPflag() & 0x01) == 1) {
            YamahaDX7SysexHelper.mkSysInfoAvail(this, (byte) (getChannel() + 0x10));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.RECEIVE_STRING);
            }
        }
        return super.editPatch(p);
    }
}
