package io.github.lightman314.lightmanscurrency.common.items;

import java.util.function.Supplier;

import io.github.lightman314.lightmanscurrency.common.upgrades.UpgradeType.UpgradeData;
import io.github.lightman314.lightmanscurrency.common.upgrades.types.capacity.CapacityUpgrade;

public class CapacityUpgradeItem extends UpgradeItem{

	private final Supplier<Integer> capacityAmount;
	
	public CapacityUpgradeItem(CapacityUpgrade upgradeType, int capacityAmount, Settings properties)
	{
		this(upgradeType, () -> capacityAmount, properties);
	}
	
	public CapacityUpgradeItem(CapacityUpgrade upgradeType, Supplier<Integer> capacityAmount, Settings properties)
	{
		super(upgradeType, properties);
		this.capacityAmount = capacityAmount;
	}

	@Override
	public void fillUpgradeData(UpgradeData data) {
		data.setValue(CapacityUpgrade.CAPACITY, Math.max(this.capacityAmount.get(), 1));
	}
	
}