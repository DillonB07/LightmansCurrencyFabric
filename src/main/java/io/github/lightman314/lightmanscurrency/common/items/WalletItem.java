package io.github.lightman314.lightmanscurrency.common.items;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.lightman314.lightmanscurrency.LCConfig;
import io.github.lightman314.lightmanscurrency.common.core.ModSounds;
import io.github.lightman314.lightmanscurrency.common.enchantments.WalletEnchantment;
import io.github.lightman314.lightmanscurrency.common.menu.factory.WalletMenuFactory;
import io.github.lightman314.lightmanscurrency.common.menu.wallet.WalletMenuBase;
import io.github.lightman314.lightmanscurrency.common.money.CoinValue;
import io.github.lightman314.lightmanscurrency.common.money.MoneyUtil;
import io.github.lightman314.lightmanscurrency.common.money.wallet.WalletHandler;
import io.github.lightman314.lightmanscurrency.network.client.messages.data.SMessageUpdateClientWallet;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import org.jetbrains.annotations.Nullable;

import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class WalletItem extends Item{
	
	private static final SoundEvent emptyOpenSound = SoundEvents.ITEM_ARMOR_EQUIP_LEATHER;
	private final Identifier MODEL_TEXTURE;
	
	private final int level;
	private final int storageSize;
	
	public WalletItem(int level, int storageSize, String modelName, Settings properties)
	{
		super(properties.maxCount(1));
		this.level = level;
		this.storageSize = storageSize;
		WalletMenuBase.updateMaxWalletSlots(this.storageSize);
		this.MODEL_TEXTURE = new Identifier(LightmansCurrency.MODID, "textures/entity/" + modelName + ".png");
	}
	
	@Override
	public int getEnchantability() { return 10; }
	
	@Override
	public boolean isEnchantable(ItemStack stack) { return true; }
	
	/**
	 * Determines if the given ItemStack can be processed as a wallet.
	 * Returns true if the stack is empty, so you will need to check for that separately.
	 */
	public static boolean validWalletStack(ItemStack walletStack)
	{
		if(walletStack.isEmpty())
			return true;
		return isWallet(walletStack.getItem());
	}
	
	/**
	 * Determines if the given ItemStack is a WalletItem
	 */
	public static boolean isWallet(ItemStack item) { return isWallet(item.getItem()); }
	
	/**
	 * Determines if the given Item is a WalletItem
	 */
	public static boolean isWallet(Item item)
	{
		return item instanceof WalletItem;
	}
	
	/**
	 * Whether the WalletItem is capable of converting coins to coins of higher value.
	 */
	public static boolean CanExchange(WalletItem wallet)
	{
		if(wallet == null)
			return false;
		return wallet.level >= LCConfig.SERVER.walletConvertLevel.get() || wallet.level >= LCConfig.SERVER.walletPickupLevel.get();
	}
	
	/**
	 * Whether the WalletItem is capable of automatically storing coins on pickup.
	 */
	public static boolean CanPickup(WalletItem wallet)
	{
		if(wallet == null)
			return false;
		return wallet.level >= LCConfig.SERVER.walletPickupLevel.get();
	}
	
	/**
	 * Whether the WalletItem is capable of interfacing with the players bank account.
	 */
	public static boolean HasBankAccess(WalletItem wallet)
	{
		if(wallet == null)
			return false;
		return wallet.level >= LCConfig.SERVER.walletBankLevel.get();
	}
	
	/**
	 * The number of inventory slots the WalletItem has.
	 */
	public static int InventorySize(WalletItem wallet)
	{
		if(wallet == null)
			return 0;
		return wallet.storageSize;
	}
	
	/**
	 * The number of inventory slots the Wallet Stack has.
	 * Returns 0 if the item is not a valid wallet.
	 */
	public static int InventorySize(ItemStack wallet)
	{
		if(wallet.getItem() instanceof WalletItem w)
			return InventorySize(w);
		return 0;
	}
	
	@Override
	public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> tooltip, TooltipContext flagIn)
	{
		
		super.appendTooltip(stack,  level,  tooltip,  flagIn);
		
		if(CanPickup(this))
		{
			tooltip.add(Text.translatable("tooltip.lightmanscurrency.wallet.pickup").formatted(Formatting.YELLOW));
		}
		if(CanExchange(this))
		{
			if(CanPickup(this))
			{
				Text onOffText = getAutoConvert(stack) ? Text.translatable("tooltip.lightmanscurrency.wallet.autoConvert.on").formatted(Formatting.GREEN) : Text.translatable("tooltip.lightmanscurrency.wallet.autoConvert.off").formatted(Formatting.RED);
				tooltip.add(Text.translatable("tooltip.lightmanscurrency.wallet.autoConvert", onOffText).formatted(Formatting.YELLOW));
			}
			else
			{
				tooltip.add(Text.translatable("tooltip.lightmanscurrency.wallet.manualConvert").formatted(Formatting.YELLOW));
			}
		}
		if(HasBankAccess(this))
		{
			tooltip.add(Text.translatable("tooltip.lightmanscurrency.wallet.bankaccount").formatted(Formatting.YELLOW));
		}
		
		WalletEnchantment.addWalletEnchantmentTooltips(tooltip, stack);
		
		CoinValue contents = new CoinValue(getWalletInventory(stack));
		if(contents.getRawValue() > 0)
			tooltip.add(Text.translatable("tooltip.lightmanscurrency.wallet.storedmoney", Text.literal(contents.getString()).formatted(Formatting.DARK_GREEN)).formatted(Formatting.YELLOW));
		
	}
	
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand)
	{
		//CurrencyMod.LOGGER.info("Wallet was used.");
		
		ItemStack wallet = player.getStackInHand(hand);
		
		if(!world.isClient)
		{
			//CurrencyMod.LOGGER.info("Opening Wallet UI?");
			
			//Determine which slot the wallet is in.
			int walletSlot = GetWalletSlot(player.getInventory(), wallet);
			
			//Open the UI
			if(walletSlot >= 0)
			{
				
				if(player.isSneaking()/* && (!LightmansCurrency.isCuriosValid(player))*/)
				{
					AtomicBoolean equippedWallet = new AtomicBoolean(false);
					WalletHandler walletHandler = WalletHandler.getWallet(player);
					if(walletHandler.getWallet().isEmpty())
					{
						walletHandler.setWallet(wallet);
						player.setStackInHand(hand, ItemStack.EMPTY);
						//Manually sync the equipped wallet so that the client container will initialize with the correct number of inventory slots
						new SMessageUpdateClientWallet(player.getUuid(), walletHandler).sendToAll();
						walletHandler.clean();
						//Flag the interaction as a success so that the wallet menu will open with the wallet in the correct slot.
						equippedWallet.set(true);
					}
					if(equippedWallet.get())
						walletSlot = -1;
				}
				player.openHandledScreen(new WalletMenuFactory(walletSlot));
			}
			else
				LightmansCurrency.LogError("Could not find the wallet in the players inventory!");
			
		}
		else
		{
			player.getWorld().playSound(player, player.getBlockPos(), emptyOpenSound, SoundCategory.PLAYERS, 0.75f, 1.25f + player.getWorld().random.nextFloat() * 0.5f);
			if(!isEmpty(wallet))
				player.getWorld().playSound(player, player.getBlockPos(), ModSounds.COINS_CLINKING, SoundCategory.PLAYERS, 0.4f, 1f);
		}
		
		return TypedActionResult.success(wallet);
		
	}
	
	/**
	 * Whether the Wallet Stacks inventory contents are empty.
	 */
	public static boolean isEmpty(ItemStack wallet)
	{
		DefaultedList<ItemStack> inventory = getWalletInventory(wallet);
		for(ItemStack stack : inventory)
		{
			if(!stack.isEmpty())
				return false;
		}
		return true;
	}
	
	private static int GetWalletSlot(Inventory inventory, ItemStack wallet)
	{
		for(int i = 0; i < inventory.size(); i++)
		{
			if(inventory.getStack(i) == wallet)
				return i;
		}
		return -1;
	}
	
	/**
	 * Places the given coin stack in the given Wallet Stack.
	 * @param wallet The wallet item stack in which to place the coin
	 * @param coins The coins to place in the wallet.
	 * @return The coins that were unable to fit in the wallet.
	 */
	public static ItemStack PickupCoin(ItemStack wallet, ItemStack coins)
	{
		
		ItemStack returnValue = coins.copy();
		
		DefaultedList<ItemStack> inventory = getWalletInventory(wallet);
		for(int i = 0; i < inventory.size() && !returnValue.isEmpty(); i++)
		{
			ItemStack thisStack = inventory.get(i);
			if(thisStack.isEmpty())
			{
				inventory.set(i, returnValue.copy());
				returnValue = ItemStack.EMPTY;
			}
			else if(thisStack.getItem() == returnValue.getItem())
			{
				int amountToAdd = MathUtil.clamp(returnValue.getCount(), 0, thisStack.getMaxCount() - thisStack.getCount());
				thisStack.setCount(thisStack.getCount() + amountToAdd);
				returnValue.setCount(returnValue.getCount() - amountToAdd);
				
			}
		}
		
		if(WalletItem.getAutoConvert(wallet))
			inventory = WalletItem.ConvertCoins(inventory);
		else
			inventory = MoneyUtil.SortCoins(inventory);
		
		putWalletInventory(wallet, inventory);
		
		//Return the coins that could not be picked up
		return returnValue;
		
	}
	
	private static DefaultedList<ItemStack> ConvertCoins(DefaultedList<ItemStack> inventory)
	{
		
		inventory = MoneyUtil.ConvertAllCoinsUp(inventory);
		
		return MoneyUtil.SortCoins(inventory);
		
	}
	
	/**
	 * Writes the given wallet inventory contents to the Wallet Stacks compound tag data.
	 */
	public static void putWalletInventory(ItemStack wallet, DefaultedList<ItemStack> inventory)
	{
		if(!(wallet.getItem() instanceof WalletItem))
			return;
		
		NbtCompound compound = wallet.getOrCreateNbt();
		NbtList invList = new NbtList();
		for(int i = 0; i < inventory.size(); i++)
		{
			ItemStack thisStack = inventory.get(i);
			if(!thisStack.isEmpty())
			{
				NbtCompound thisItemCompound = thisStack.writeNbt(new NbtCompound());
				thisItemCompound.putByte("Slot", (byte)i);
				invList.add(thisItemCompound);
			}
		}
		compound.put("Items", invList);
		wallet.setNbt(compound);
	}
	
	/**
	 * Reads & returns the wallets inventory contents from the ItemStack's compound tag data.
	 */
	public static DefaultedList<ItemStack> getWalletInventory(ItemStack wallet)
	{
		
		NbtCompound compound = wallet.getOrCreateNbt();
		 if(!(wallet.getItem() instanceof WalletItem))
			 return DefaultedList.ofSize(6, ItemStack.EMPTY);

		DefaultedList<ItemStack> value = DefaultedList.ofSize(WalletItem.InventorySize((WalletItem)wallet.getItem()), ItemStack.EMPTY);
		if(!compound.contains("Items"))
			return value;
		
		NbtList invList = compound.getList("Items", NbtElement.COMPOUND_TYPE);
		for(int i = 0; i < invList.size(); i++)
		{
			NbtCompound thisCompound = invList.getCompound(i);
			ItemStack thisStack = ItemStack.fromNbt(thisCompound);
			int j = (int)thisCompound.getByte("Slot") & 255;
			if(j < value.size())
				value.set(j, thisStack);
		}
		
		return value;
		
	}
	
	/**
	 * Gets the auto-convert state of the given Wallet Stack.
	 * Returns false if the wallet is not capable of both converting & collecting coins.
	 */
	public static boolean getAutoConvert(ItemStack wallet)
	{
		if(!(wallet.getItem() instanceof WalletItem))
			return false;
		
		if(!WalletItem.CanExchange((WalletItem)wallet.getItem()) || !WalletItem.CanPickup((WalletItem)wallet.getItem()))
			return false;
		
		NbtCompound tag = wallet.getOrCreateNbt();
		if(!tag.contains("AutoConvert"))
		{
			tag.putBoolean("AutoConvert", true);
			return true;
		}
		
		return tag.getBoolean("AutoConvert");
		
	}
	
	/**
	 * Toggles the auto-convert state of the given Wallet Stack.
	 * Does nothing if the wallet is not capable of both converting & collecting coins.
	 */
	public static void toggleAutoConvert(ItemStack wallet)
	{
		
		if(!(wallet.getItem() instanceof WalletItem))
			return;
		
		if(!WalletItem.CanExchange((WalletItem)wallet.getItem()))
			return;
		
		NbtCompound tag = wallet.getOrCreateNbt();
		boolean oldValue = WalletItem.getAutoConvert(wallet);
		tag.putBoolean("AutoConvert", !oldValue);
		
	}
	
	/**
	 * Used to copy a wallets inventory contents to a newly crafted one. Also copies over any auto-conversion settings, custom names, and enchantments.
	 * @param walletIn The wallet inventory being copied.
	 * @param walletOut The wallet whose inventory will be filled
	 */
	public static void CopyWalletContents(ItemStack walletIn, ItemStack walletOut)
	{
		if(!(walletIn.getItem() instanceof WalletItem && walletIn.getItem() instanceof WalletItem))
		{
			LightmansCurrency.LogError("WalletItem.CopyWalletContents() -> One or both of the wallet stacks are not WalletItems.");
			return;
		}
		WalletItem walletItemIn = (WalletItem)walletIn.getItem();
		WalletItem walletItemOut = (WalletItem)walletOut.getItem();
		DefaultedList<ItemStack> walletInventory1 = getWalletInventory(walletIn);
		DefaultedList<ItemStack> walletInventory2 = getWalletInventory(walletOut);
		if(walletInventory1.size() > walletInventory2.size())
			LightmansCurrency.LogWarning("WalletItem.CopyWalletContents() -> walletIn has a larger inventory size than walletOut. This may result in a loss of wallet contents.");
		//Copy over the wallets contents
		for(int i = 0; i < walletInventory1.size() && i < walletInventory2.size(); i++)
		{
			walletInventory2.set(i, walletInventory1.get(i).copy());
		}
		//Write walletOut's nbt data
		putWalletInventory(walletOut, walletInventory2);
		//If both wallets can convert, confirm that the auto-convert setting matches
		if(CanExchange(walletItemIn) && CanExchange(walletItemOut) && CanPickup(walletItemIn) && CanPickup(walletItemOut))
		{
			if(getAutoConvert(walletIn) != getAutoConvert(walletOut))
			{
				toggleAutoConvert(walletOut);
			}
		}
		
		//Copy custom name
		if(walletIn.hasCustomName())
			walletOut.setCustomName(walletIn.getName());
		
		//Copy enchantments
		EnchantmentHelper.set(EnchantmentHelper.get(walletIn), walletOut);
		
	}
	
	/**
	 * The wallets texture. Used to render the wallet on the players hip when equipped.
	 */
	public Identifier getModelTexture()
	{
		return this.MODEL_TEXTURE;
	}
	
}