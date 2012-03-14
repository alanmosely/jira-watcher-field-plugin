package com.burningcode.jira.plugin;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.HashMap;

import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;
import com.opensymphony.module.propertyset.map.MapPropertySet;

import junit.framework.TestCase;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertySetManager.class)

public class TestWatcherFieldSettings extends TestCase {
	
	@Override
	protected void setUp() throws Exception {
		PowerMockito.mockStatic(PropertySetManager.class);
		MapPropertySet propertySet = new MapPropertySet();
		propertySet.setMap(new HashMap<String, Object>());
		when(PropertySetManager.getInstance(eq("ofbiz"), anyMap())).thenReturn(propertySet);

		super.setUp();
	}

	
	public void testStaticGetPropertySet(){
		PropertySet propertySet = WatcherFieldSettings.getPropertySet();
		assertTrue(propertySet.exists("ignorePermissions"));
		assertTrue(propertySet.getObject("ignorePermissions") instanceof Boolean);
		assertTrue(!propertySet.getBoolean("ignorePermissions"));

		propertySet.setBoolean("ignorePermissions", true);
		propertySet = WatcherFieldSettings.getPropertySet();
		assertTrue(propertySet.getObject("ignorePermissions") instanceof Boolean);
		assertTrue(propertySet.getBoolean("ignorePermissions"));
	}	
}
