package android.location.cts;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.ToBeFixed;
import android.location.Criteria;
import android.os.Parcel;
import android.test.AndroidTestCase;

@TestTargetClass(Criteria.class)
public class CriteriaTest extends AndroidTestCase {

    @TestTargets({ @TestTargetNew(level = TestLevel.COMPLETE, method = "Criteria", args = {  }), @TestTargetNew(level = TestLevel.COMPLETE, method = "Criteria", args = { android.location.Criteria.class }) })
    @ToBeFixed(bug = "1695243", explanation = "Android API javadocs are incomplete, " + "should add @throw NullPointerException into javadoc when the parameter is null.")
    public void testConstructor() {
        new Criteria();
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        c.setAltitudeRequired(true);
        c.setBearingRequired(true);
        c.setCostAllowed(true);
        c.setPowerRequirement(Criteria.POWER_HIGH);
        c.setSpeedRequired(true);
        Criteria criteria = new Criteria(c);
        assertEquals(Criteria.ACCURACY_FINE, criteria.getAccuracy());
        assertTrue(criteria.isAltitudeRequired());
        assertTrue(criteria.isBearingRequired());
        assertTrue(criteria.isCostAllowed());
        assertTrue(criteria.isSpeedRequired());
        assertEquals(Criteria.POWER_HIGH, criteria.getPowerRequirement());
        try {
            new Criteria(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(level = TestLevel.COMPLETE, method = "describeContents", args = {  })
    public void testDescribeContents() {
        Criteria criteria = new Criteria();
        criteria.describeContents();
    }

    @TestTargets({ @TestTargetNew(level = TestLevel.COMPLETE, method = "setAccuracy", args = { int.class }), @TestTargetNew(level = TestLevel.COMPLETE, method = "getAccuracy", args = {  }) })
    @ToBeFixed(bug = "1728526", explanation = "setAccuracy did not throw " + "IllegalArgumentException when argument is negative.")
    public void testAccessAccuracy() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        assertEquals(Criteria.ACCURACY_FINE, criteria.getAccuracy());
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        assertEquals(Criteria.ACCURACY_COARSE, criteria.getAccuracy());
        try {
            criteria.setAccuracy(-1);
        } catch (IllegalArgumentException e) {
        }
        try {
            criteria.setAccuracy(Criteria.ACCURACY_COARSE + 1);
        } catch (IllegalArgumentException e) {
        }
    }

    @TestTargets({ @TestTargetNew(level = TestLevel.COMPLETE, method = "setPowerRequirement", args = { int.class }), @TestTargetNew(level = TestLevel.COMPLETE, method = "getPowerRequirement", args = {  }) })
    @ToBeFixed(bug = "1695243", explanation = "should add @throws IllegalArgumentException " + "clause into javadoc of setPowerRequirement() when input is valid.")
    public void testAccessPowerRequirement() {
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
        assertEquals(Criteria.NO_REQUIREMENT, criteria.getPowerRequirement());
        criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
        assertEquals(Criteria.POWER_MEDIUM, criteria.getPowerRequirement());
        try {
            criteria.setPowerRequirement(-1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            criteria.setPowerRequirement(Criteria.POWER_HIGH + 1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @TestTargets({ @TestTargetNew(level = TestLevel.COMPLETE, method = "setAltitudeRequired", args = { boolean.class }), @TestTargetNew(level = TestLevel.COMPLETE, method = "isAltitudeRequired", args = {  }) })
    public void testAccessAltitudeRequired() {
        Criteria criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        assertFalse(criteria.isAltitudeRequired());
        criteria.setAltitudeRequired(true);
        assertTrue(criteria.isAltitudeRequired());
    }

    @TestTargets({ @TestTargetNew(level = TestLevel.COMPLETE, method = "setBearingRequired", args = { boolean.class }), @TestTargetNew(level = TestLevel.COMPLETE, method = "isBearingRequired", args = {  }) })
    public void testAccessBearingRequired() {
        Criteria criteria = new Criteria();
        criteria.setBearingRequired(false);
        assertFalse(criteria.isBearingRequired());
        criteria.setBearingRequired(true);
        assertTrue(criteria.isBearingRequired());
    }

    @TestTargets({ @TestTargetNew(level = TestLevel.COMPLETE, method = "setCostAllowed", args = { boolean.class }), @TestTargetNew(level = TestLevel.COMPLETE, method = "isCostAllowed", args = {  }) })
    public void testAccessCostAllowed() {
        Criteria criteria = new Criteria();
        criteria.setCostAllowed(false);
        assertFalse(criteria.isCostAllowed());
        criteria.setCostAllowed(true);
        assertTrue(criteria.isCostAllowed());
    }

    @TestTargets({ @TestTargetNew(level = TestLevel.COMPLETE, method = "setSpeedRequired", args = { boolean.class }), @TestTargetNew(level = TestLevel.COMPLETE, method = "isSpeedRequired", args = {  }) })
    public void testAccessSpeedRequired() {
        Criteria criteria = new Criteria();
        criteria.setSpeedRequired(false);
        assertFalse(criteria.isSpeedRequired());
        criteria.setSpeedRequired(true);
        assertTrue(criteria.isSpeedRequired());
    }

    @TestTargetNew(level = TestLevel.COMPLETE, notes = "this function does not read parameter 'flag'.", method = "writeToParcel", args = { android.os.Parcel.class, int.class })
    public void testWriteToParcel() {
        Criteria criteria = new Criteria();
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setSpeedRequired(true);
        Parcel parcel = Parcel.obtain();
        criteria.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Criteria newCriteria = Criteria.CREATOR.createFromParcel(parcel);
        assertEquals(criteria.getAccuracy(), newCriteria.getAccuracy());
        assertEquals(criteria.getPowerRequirement(), newCriteria.getPowerRequirement());
        assertEquals(criteria.isAltitudeRequired(), newCriteria.isAltitudeRequired());
        assertEquals(criteria.isBearingRequired(), newCriteria.isBearingRequired());
        assertEquals(criteria.isSpeedRequired(), newCriteria.isSpeedRequired());
        assertEquals(criteria.isCostAllowed(), newCriteria.isCostAllowed());
    }
}
