package net.nerdypuzzle.geckolib.element.types;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;

import net.mcreator.element.BaseType;
import net.mcreator.element.GeneratableElement;
import net.mcreator.element.parts.BiomeEntry;
import net.mcreator.element.parts.Fluid;
import net.mcreator.element.parts.IWorkspaceDependent;
import net.mcreator.element.parts.MItemBlock;
import net.mcreator.element.parts.Sound;
import net.mcreator.element.parts.StepSound;
import net.mcreator.element.parts.TabEntry;
import net.mcreator.element.parts.TextureHolder;
import net.mcreator.element.parts.procedure.NumberProcedure;
import net.mcreator.element.parts.procedure.Procedure;
import net.mcreator.element.parts.procedure.StringListProcedure;
import net.mcreator.element.types.interfaces.IBlock;
import net.mcreator.element.types.interfaces.IBlockWithBoundingBox;
import net.mcreator.element.types.interfaces.ITabContainedElement;
import net.mcreator.generator.GeneratorFlavor;
import net.mcreator.minecraft.MCItem;
import net.mcreator.minecraft.MinecraftImageGenerator;
import net.mcreator.ui.workspace.resources.TextureType;
import net.mcreator.util.image.ImageUtils;
import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.references.ModElementReference;
import net.mcreator.workspace.references.TextureReference;

/**
 * Versão compatível com Java 17+ sem dependências de anotação externas.
 */
public class AnimatedBlock extends GeneratableElement implements IBlock, ITabContainedElement, IBlockWithBoundingBox {

    public TextureHolder texture;
    public TextureHolder textureTop;
    public TextureHolder textureLeft;
    public TextureHolder textureFront;
    public TextureHolder textureRight;
    public TextureHolder textureBack;
    public int renderType;
    public int rotationMode;
    public boolean enablePitch;
    public boolean emissiveRendering;
    public boolean displayFluidOverlay;
    public boolean animateBlockItem;
    public TextureHolder itemTexture;
    public TextureHolder particleTexture;
    public String blockBase;
    public String tintType;
    public boolean isItemTinted;
    public boolean hasTransparency;
    public boolean connectedSides;
    public String transparencyType;
    public boolean disableOffset;
    public List<BoxEntry> boundingBoxes;
    public String name;
    public StringListProcedure specialInformation;
    public double hardness;
    public double resistance;
    public boolean hasGravity;
    public boolean isWaterloggable;
    public TabEntry creativeTab;
    public List<TabEntry> creativeTabs;
    public String destroyTool;
    public MItemBlock customDrop;
    public int dropAmount;
    public boolean useLootTableForDrops;
    public boolean requiresCorrectTool;
    public double enchantPowerBonus;
    public boolean plantsGrowOn;
    public boolean canRedstoneConnect;
    public int lightOpacity;
    public String material; // ← corrigido
    public int tickRate;
    public boolean tickRandomly;
    public boolean isReplaceable;
    public boolean canProvidePower;
    public NumberProcedure emittedRedstonePower;
    public String colorOnMap;
    public MItemBlock creativePickItem;
    public String offsetType;
    public String aiPathNodeType;
    public Color beaconColorModifier;
    public int flammability;
    public int fireSpreadSpeed;
    public boolean isLadder;
    public double slipperiness;
    public double speedFactor;
    public double jumpFactor;
    public Number animationCount;
    public String reactionToPushing;
    public boolean isNotColidable;
    public boolean isCustomSoundType;
    public StepSound soundOnStep;
    public Sound breakSound;
    public Sound fallSound;
    public Sound hitSound;
    public Sound placeSound;
    public Sound stepSound;
    public int luminance;
    public boolean unbreakable;
    public int breakHarvestLevel;
    public Procedure placingCondition;
    public boolean hasInventory;
    public String guiBoundTo;
    public boolean openGUIOnRightClick;
    public int inventorySize;
    public int inventoryStackSize;
    public boolean inventoryDropWhenDestroyed;
    public boolean inventoryComparatorPower;
    public boolean generateFeature;
    public List<Integer> inventoryOutSlotIDs;
    public List<Integer> inventoryInSlotIDs;
    public boolean hasEnergyStorage;
    public int energyInitial;
    public int energyCapacity;
    public int energyMaxReceive;
    public int energyMaxExtract;
    public boolean isFluidTank;
    public int fluidCapacity;
    public List<Fluid> fluidRestrictions;
    public Procedure onRightClicked;
    public Procedure onBlockAdded;
    public Procedure onNeighbourBlockChanges;
    public Procedure onTickUpdate;
    public Procedure onRandomUpdateEvent;
    public Procedure onDestroyedByPlayer;
    public Procedure onDestroyedByExplosion;
    public Procedure onStartToDestroy;
    public Procedure onEntityCollides;
    public Procedure onEntityWalksOn;
    public Procedure onBlockPlayedBy;
    public Procedure onRedstoneOn;
    public Procedure onRedstoneOff;
    public Procedure onHitByProjectile;

    public List<BiomeEntry> restrictionBiomes;
    @ModElementReference
    public List<MItemBlock> blocksToReplace;
    public String generationShape;
    public int frequencyPerChunks;
    public int frequencyOnChunk;
    public int minGenerateHeight;
    public int maxGenerateHeight;
    public Procedure generateCondition;
    public String normal;
    public String displaySettings;
    public String vanillaToolTier;
    public Procedure additionalHarvestCondition;

    public List<BlockstateListEntry> blockstateList = new ArrayList<>();

    public static class BlockstateListEntry implements IBlockWithBoundingBox {
        public BlockstateListEntry() {
            boundingBoxes = new ArrayList<>();
        }

        @TextureReference(TextureType.BLOCK)
        public TextureHolder texture;
        @TextureReference(TextureType.BLOCK)
        public TextureHolder particleTexture;
        public int renderType;
        public String customModelName;
        public int luminance;
        public List<IBlockWithBoundingBox.BoxEntry> boundingBoxes;

        @Override
        public List<BoxEntry> getValidBoundingBoxes() {
            return boundingBoxes.stream().filter(IBlockWithBoundingBox.BoxEntry::isNotEmpty).collect(Collectors.toList());
        }
    }

    public static class BlockstateEntry implements IWorkspaceDependent {
        public TextureHolder texture;
        public TextureHolder particleTexture;
        public String customModelName;

        transient Workspace workspace;

        @Override
        public void setWorkspace(Workspace workspace) {
            this.workspace = workspace;
        }

        @Override
        public Workspace getWorkspace() {
            return this.workspace;
        }
    }

    public List<BlockstateEntry> getBlockstates() {
        List<BlockstateEntry> entries = new ArrayList<>();
        for (BlockstateListEntry state : blockstateList) {
            BlockstateEntry entry = new BlockstateEntry();
            entry.setWorkspace(getModElement().getWorkspace());
            entry.particleTexture = state.particleTexture;
            entry.texture = state.texture;
            entry.customModelName = state.customModelName;
            entries.add(entry);
        }
        return entries;
    }

    private AnimatedBlock() {
        this((ModElement) null);
    }

    public AnimatedBlock(ModElement element) {
        super(element);
        this.tintType = "No tint";
        this.boundingBoxes = new ArrayList<>();
        this.restrictionBiomes = new ArrayList<>();
        this.blocksToReplace = new ArrayList<>();
        this.reactionToPushing = "NORMAL";
        this.slipperiness = 0.6;
        this.speedFactor = 1.0;
        this.jumpFactor = 1.0;
        this.colorOnMap = "DEFAULT";
        this.aiPathNodeType = "DEFAULT";
        this.offsetType = "NONE";
        this.generationShape = "UNIFORM";
        this.inventoryInSlotIDs = new ArrayList<>();
        this.inventoryOutSlotIDs = new ArrayList<>();
        this.energyCapacity = 400000;
        this.energyMaxReceive = 200;
        this.energyMaxExtract = 200;
        this.fluidCapacity = 8000;
        this.animationCount = 1;
        this.vanillaToolTier = "NONE";
        this.creativeTabs = new ArrayList<>();
    }

    public int renderType() {
        return this.blockBase != null && !this.blockBase.isEmpty() ? -1 : this.renderType;
    }

    public boolean hasCustomDrop() {
        return !this.customDrop.isEmpty();
    }

    public boolean isBlockTinted() {
        return !"No tint".equals(this.tintType);
    }

    public boolean isDoubleBlock() {
        return "Door".equals(this.blockBase);
    }

    public boolean shouldOpenGUIOnRightClick() {
        return this.guiBoundTo != null && !this.guiBoundTo.equals("<NONE>") && this.openGUIOnRightClick;
    }

    public boolean shouldScheduleTick() {
        return this.tickRate > 0 && !this.tickRandomly;
    }

    public boolean shouldDisableOffset() {
        return this.disableOffset || this.offsetType.equals("NONE");
    }

    public boolean isFullCube() {
        return !"Stairs".equals(this.blockBase)
                && !"Slab".equals(this.blockBase)
                && !"Fence".equals(this.blockBase)
                && !"Wall".equals(this.blockBase)
                && !"TrapDoor".equals(this.blockBase)
                && !"Door".equals(this.blockBase)
                && !"FenceGate".equals(this.blockBase)
                && !"EndRod".equals(this.blockBase)
                && !"PressurePlate".equals(this.blockBase)
                && !"Button".equals(this.blockBase)
                && IBlockWithBoundingBox.super.isFullCube();
    }

    public List<TabEntry> getCreativeTabs() {
        if (creativeTab != null && !creativeTab.isEmpty())
            return List.of(creativeTab);
        return creativeTabs;
    }

    public List<IBlockWithBoundingBox.BoxEntry> getValidBoundingBoxes() {
        return boundingBoxes.stream().filter(IBlockWithBoundingBox.BoxEntry::isNotEmpty).collect(Collectors.toList());
    }

    @Override
    public List<MCItem> providedMCItems() {
        return List.of(new MCItem.Custom(this.getModElement(), null, "block"));
    }

    @Override
    public List<MCItem> getCreativeTabItems() {
        return providedMCItems();
    }

    public BufferedImage generateModElementPicture() {
        if (this.renderType() == 10) {
            return (BufferedImage) MinecraftImageGenerator.Preview.generateBlockIcon(
                    getTextureWithFallback(this.textureTop),
                    getTextureWithFallback(this.textureLeft),
                    getTextureWithFallback(this.textureFront));
        } else if (this.renderType() == 11 || this.renderType() == 110
                || "Leaves".equals(this.blockBase)) {
            return (BufferedImage) MinecraftImageGenerator.Preview.generateBlockIcon(
                    getMainTexture(), getMainTexture(), getMainTexture());
        } else if ("Slab".equals(this.blockBase)) {
            return (BufferedImage) MinecraftImageGenerator.Preview.generateSlabIcon(
                    getTextureWithFallback(this.textureTop),
                    getTextureWithFallback(this.textureFront));
        } else if ("TrapDoor".equals(this.blockBase)) {
            return (BufferedImage) MinecraftImageGenerator.Preview.generateTrapdoorIcon(getMainTexture());
        } else if ("Stairs".equals(this.blockBase)) {
            return (BufferedImage) MinecraftImageGenerator.Preview.generateStairsIcon(
                    getTextureWithFallback(this.textureTop),
                    getTextureWithFallback(this.textureFront));
        } else if ("Wall".equals(this.blockBase)) {
            return (BufferedImage) MinecraftImageGenerator.Preview.generateWallIcon(getMainTexture());
        } else if ("Fence".equals(this.blockBase)) {
            return (BufferedImage) MinecraftImageGenerator.Preview.generateFenceIcon(getMainTexture());
        } else if ("FenceGate".equals(this.blockBase)) {
            return (BufferedImage) MinecraftImageGenerator.Preview.generateFenceGateIcon(getMainTexture());
        } else if ("EndRod".equals(this.blockBase)) {
            return (BufferedImage) MinecraftImageGenerator.Preview.generateEndRodIcon(getMainTexture());
        } else if ("PressurePlate".equals(this.blockBase)) {
            return (BufferedImage) MinecraftImageGenerator.Preview.generatePressurePlateIcon(getMainTexture());
        } else if ("Button".equals(this.blockBase)) {
            return (BufferedImage) MinecraftImageGenerator.Preview.generateButtonIcon(getMainTexture());
        } else if (this.renderType() == 14) {
            Image side = ImageUtils.drawOver(
                    new ImageIcon(getTextureWithFallback(this.textureFront)),
                    new ImageIcon(getTextureWithFallback(this.textureLeft))).getImage();
            return (BufferedImage) MinecraftImageGenerator.Preview.generateBlockIcon(
                    getTextureWithFallback(this.textureTop), side, side);
        } else {
            return ImageUtils.resizeAndCrop(getMainTexture(), 32);
        }
    }

    private Image getMainTexture() {
        return texture.getImage(TextureType.BLOCK);
    }

    private Image getTextureWithFallback(TextureHolder texture) {
        if (texture.isEmpty())
            return getMainTexture();
        return texture.getImage(TextureType.BLOCK);
    }

    public String getRenderType() {
        return this.hasTransparency && "solid".equals(this.transparencyType)
                ? "cutout"
                : this.transparencyType.toLowerCase(Locale.ENGLISH);
    }

    public boolean hasBlockstates() {
        return !blockstateList.isEmpty();
    }

    public Collection<BaseType> getBaseTypesProvided() {
        List<BaseType> baseTypes = new ArrayList<>(List.of(BaseType.BLOCK, BaseType.ITEM, BaseType.BLOCKENTITY));
        if (generateFeature && getModElement().getGenerator().getGeneratorConfiguration().getGeneratorFlavor()
                == GeneratorFlavor.FABRIC) {
            baseTypes.add(BaseType.FEATURE);
        }
        return baseTypes;
    }
}
