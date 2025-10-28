package builderb0y.bigpixel;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;

public class OrganizedSelection<
	T_Value    extends                    OrganizedSelection.Value<T_Type>,
	T_Type     extends Enum<T_Type>     & OrganizedSelection.Type<T_Value, T_Category>,
	T_Category extends Enum<T_Category> & OrganizedSelection.Category<T_Category>
> {

	public LayerNode layer;
	public Map<String, T_Type> saveNameToType;
	public EnumMap<T_Type, RadioMenuItem> typeToMenuItem;
	public EnumMap<T_Type, T_Value> typeToValue;
	public EnumMap<T_Category, ObservableList<MenuItem>> categoryToMenu;
	public ToggleGroup toggleGroup;
	public MenuButton rootButton;
	public ObjectBinding<T_Type> selectedType;
	public ObjectBinding<T_Value> selectedValue;

	@SuppressWarnings("unchecked")
	public OrganizedSelection(LayerNode layer, Class<T_Type> typeClass, Class<T_Category> categoryClass) {
		this.layer = layer;
		T_Type[] types = typeClass.getEnumConstants();
		this.saveNameToType = new HashMap<>(types.length);
		this.typeToMenuItem = new EnumMap<>(typeClass);
		this.typeToValue = new EnumMap<>(typeClass);
		this.categoryToMenu = new EnumMap<>(categoryClass);
		this.toggleGroup = new ToggleGroup();
		this.rootButton = new MenuButton();

		for (T_Type type : types) {
			this.saveNameToType.put(type.getSaveName(), type);
			RadioMenuItem item = new RadioMenuItem(type.toString());
			item.setUserData(type);
			item.setToggleGroup(this.toggleGroup);
			this.typeToMenuItem.put(type, item);
			this.getOrCreateCategory(type.getCategory()).add(item);
		}
		this.select(types[0]);

		this.selectedType = (ObjectBinding<T_Type>)(
			this.toggleGroup.selectedToggleProperty().map((Toggle toggle) -> (
				(T_Type)(
					(
						(MenuItem)(
							toggle
						)
					)
					.getUserData()
				)
			))
		);
		this.selectedValue = (ObjectBinding<T_Value>)(
			this.selectedType.map(this::getOrCreateValue)
		);
		this.rootButton.textProperty().bind(
			this.selectedType.asString()
		);
		this.selectedType.addListener(Util.change(
			(T_Type oldType, T_Type newType) -> {
				T_Value oldSource = this.typeToValue.get(oldType);
				if (oldSource != null) {
					oldSource.getDependencies().getCurves().forEach((CurveHelper helper) -> helper.selfSourceIsSelected.set(false));
				}
				T_Value newSource = this.typeToValue.get(newType);
				if (newSource != null) {
					newSource.getDependencies().getCurves().forEach((CurveHelper helper) -> helper.selfSourceIsSelected.set(true));
				}
			}
		));
	}

	public JsonMap save() {
		return this.selectedValue.get().save();
	}

	public void load(JsonMap saveData) {
		String typeName = saveData.getString("type");
		T_Type type = this.saveNameToType.get(typeName);
		if (type == null) throw new SaveException("Unknown type: " + typeName);
		this.getOrCreateValue(type).load(saveData);
		this.select(type);
	}

	public void select(T_Type type) {
		this.toggleGroup.selectToggle(this.typeToMenuItem.get(type));
	}

	public ObservableList<MenuItem> getOrCreateCategory(T_Category category) {
		ObservableList<MenuItem> existing = this.categoryToMenu.get(category);
		if (existing != null) return existing;

		T_Category parent = category.getParent();
		if (parent != null) {
			Menu menu = new Menu(category.toString());
			menu.setUserData(category);
			existing = menu.getItems();
			this.categoryToMenu.put(category, existing);
			this.getOrCreateCategory(parent).add(menu);
			return existing;
		}
		else {
			return this.rootButton.getItems();
		}
	}

	public T_Value getOrCreateValue(T_Type type) {
		return this.typeToValue.computeIfAbsent(type, this::createValue);
	}

	public T_Value createValue(T_Type type) {
		return type.create(this.layer);
	}

	public void setPotentialInputs(List<LayerNode> layers) {
		for (T_Value source : this.typeToValue.values()) {
			source.setPossibleDependencies(layers);
		}
	}

	public static interface Value<T_Type> {

		public abstract JsonMap save();

		public abstract void load(JsonMap root);

		public abstract LayerNode getLayer();

		public abstract LayerDependencies getDependencies();

		public default void setPossibleDependencies(List<LayerNode> dependencies) {
			this.getDependencies().retainAll(dependencies);
		}

		public abstract void redrawLater();

		public abstract T_Type getType();
	}

	public static interface Type<T_Value, T_Category extends Category<T_Category>> {

		public abstract T_Value create(LayerNode layer);

		public abstract T_Category getCategory();

		@Override
		public abstract String toString();

		public abstract String getSaveName();
	}

	public static interface Category<T extends Category<T>> {

		public abstract @Nullable T getParent();

		@Override
		public abstract String toString();
	}
}