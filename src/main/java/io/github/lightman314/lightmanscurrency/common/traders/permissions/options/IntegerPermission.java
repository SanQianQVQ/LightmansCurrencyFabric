package io.github.lightman314.lightmanscurrency.common.traders.permissions.options;

import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;

public class IntegerPermission extends PermissionOption{

    public final int maxValue;

    protected IntegerPermission(String permission, int maxValue) {
        super(permission);
        this.maxValue = Math.abs(maxValue);
    }

    @Override
    protected void createWidget(int x, int y, OptionWidgets widgets) {
        LightmansCurrency.LogWarning("Integer Permission Widget is not yet built.");
    }

    @Override
    public void tick() {


    }

    @Override
    public int widgetWidth() {
        return 0;
    }

    public static IntegerPermission of(String permission, int maxValue) { return new IntegerPermission(permission, maxValue); }

}