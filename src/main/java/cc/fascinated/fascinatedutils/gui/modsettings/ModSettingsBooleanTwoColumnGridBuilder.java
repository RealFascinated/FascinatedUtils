package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.widgets.*;

import java.util.List;

public final class ModSettingsBooleanTwoColumnGridBuilder {

    private ModSettingsBooleanTwoColumnGridBuilder() {
    }

    /**
     * Builds paired cells for consecutive boolean settings (two columns, row-major fill: first column fills, then second).
     *
     * @param gridInnerWidth full width available to the grid
     * @param settings       one or more boolean settings
     * @param cellHeight     outer height per cell
     * @param cellFactory    produces each cell widget for a setting and column width
     * @return column host spanning {@code gridInnerWidth}
     */
    public static FWidget build(float gridInnerWidth, List<BooleanSetting> settings, float cellHeight, CellFactory cellFactory) {
        if (settings.isEmpty()) {
            throw new IllegalArgumentException("grid requires at least one boolean setting");
        }
        float columnGap = SettingsUiMetrics.BOOLEAN_GRID_COLUMN_GAP_DESIGN;
        float rowGap = SettingsUiMetrics.BOOLEAN_GRID_ROW_GAP_DESIGN;
        float usable = Math.max(1f, gridInnerWidth - columnGap);
        float cellWidth = usable * 0.5f;
        int count = settings.size();
        int leftColumnCount = (count + 1) / 2;
        FColumnWidget column = new FColumnWidget(rowGap, Align.CENTER);
        for (int row = 0; row < leftColumnCount; row++) {
            FRowWidget rowWidget = new FRowWidget(columnGap, Align.CENTER);
            rowWidget.addChild(cellFactory.create(settings.get(row), cellWidth, cellHeight));
            int rightIndex = row + leftColumnCount;
            if (rightIndex < count) {
                rowWidget.addChild(cellFactory.create(settings.get(rightIndex), cellWidth, cellHeight));
            }
            else {
                rowWidget.addChild(new FSpacerWidget(cellWidth, cellHeight));
            }
            column.addChild(rowWidget);
        }
        return new FMinWidthHostWidget(gridInnerWidth, column);
    }

    /**
     * Produces one cell in a {@link #build} grid row.
     */
    @FunctionalInterface
    public interface CellFactory {

        /**
         * Creates a single grid cell for a boolean setting.
         *
         * @param setting    boolean setting bound to this cell
         * @param cellWidth  width allocated to the column
         * @param cellHeight outer height of the cell row
         * @return widget laid out at the given dimensions
         */
        FWidget create(BooleanSetting setting, float cellWidth, float cellHeight);
    }
}

