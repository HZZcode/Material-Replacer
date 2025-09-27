package material_replacer.zz_404;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaterialReplacer implements ModInitializer {
    public static final String MOD_ID = "material-replacer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ReplaceMaterialCommand.register();
    }
}