package icbm.classic.datafix;

import com.adelean.inject.resources.junit.jupiter.GivenJsonResource;
import com.adelean.inject.resources.junit.jupiter.TestWithResources;
import icbm.classic.TestBase;
import icbm.classic.content.items.ItemMissile;
import icbm.classic.content.reg.ItemReg;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@TestWithResources
public class EntityMissileDataFixerTest extends TestBase {

    @GivenJsonResource("data/saves/4.0.0/entity_missile_ballistic.json")
    NBTTagCompound ballistic400save;

    @GivenJsonResource("data/saves/4.0.0/entity_missile_rpg.json")
    NBTTagCompound rpg400save;

    @GivenJsonResource("data/saves/fixer/entity_missile_ballistic.json")
    NBTTagCompound ballistic420save;

    @GivenJsonResource("data/saves/fixer/entity_missile_rpg.json")
    NBTTagCompound rpg420save;

    final EntityMissileDataFixer dataFixer = new EntityMissileDataFixer();

    @BeforeAll
    public static void beforeAllTests()
    {
        // Register block for placement
        ForgeRegistries.ITEMS.register(ItemReg.itemExplosiveMissile = new ItemMissile());
    }

    @Test
    @DisplayName("Updates v4.0.0 ballastic missile save")
    void loadFromVersion4_ballistic() {

        // Check that we have saves
        Assertions.assertNotNull(ballistic400save);
        Assertions.assertNotNull(ballistic420save);

        // Modify expected to ignore fields we don't convert but a normal save would still have
        DataFixerHelpers.removeNestedTag(ballistic420save, "missile", "flight", "data", "calculated");
        DataFixerHelpers.removeNestedTag(ballistic420save, "missile", "flight", "data", "timers", "climb_height");
        DataFixerHelpers.removeNestedTag(ballistic420save, "missile", "source", "data", "dimension");

        final NBTTagCompound updatedSave = dataFixer.fixTagCompound(ballistic400save);

        assertTags(ballistic420save, updatedSave);
    }

    @Test
    @DisplayName("Updates v4.0.0 rpg missile save")
    void loadFromVersion4_rpg() {

        // Check that we have saves
        Assertions.assertNotNull(rpg400save);
        Assertions.assertNotNull(rpg420save);

        // Modify expected to ignore fields we don't convert but a normal save would still have
        DataFixerHelpers.removeNestedTag(rpg420save, "missile", "source", "data", "dimension");
        DataFixerHelpers.removeNestedTag(rpg420save, "health");
        DataFixerHelpers.removeNestedTag(rpg420save, "missile", "source", "data", "entity");

        final NBTTagCompound updatedSave = dataFixer.fixTagCompound(rpg400save);

        assertTags(rpg420save, updatedSave);
    }
}
