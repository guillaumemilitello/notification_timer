package com.notification.timer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;

public class PresetCardsList extends Fragment {

    private static final String TAG = "PresetCardsList";

    private MainActivity mainActivity;

    private PresetsList presetsList;
    private int presetsListSize = 0;

    private Preset presetUser;

    private static final int USER_POSITION_NONE = -1;
    private int userPosition;

    private static final int ITEM_VIEW_TYPE_PRESET = 1;

    private LinearLayoutManager linearLayoutManager;
    private RecycleViewAdapter adapter;
    private boolean addPresetButton;

    private Context context;
    private AlertDialog deleteAlertDialog;
    private AlertDialog selectAlertDialog;

    private SharedPreferences sharedPreferences;

    private void addPreset() {
        if (presetsList.indexOf(presetUser) == -1) {
            presetsListSize = presetsList.addPreset(0, presetUser);
            Log.d(TAG, "addPreset: presetUser=" + presetUser + ", presetList=" + presetsList);
            addPresetButton = false;
            notifyItemChanged(0);
            scrollToPosition(0);
        } else {
            Log.e(TAG, "addPreset: presetUser=" + presetUser + ", presetList=" + presetsList + " already exists, index=" + presetsList.indexOf(presetUser));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            mainActivity.updateShortcuts();
        }
    }

    private void removePreset(int position) {
        int index = getListIndex(position);
        Preset preset = presetsList.getPreset(index);
        Log.d(TAG, "removePreset: position=" + position + ", index=" + index + ", presetList=" + presetsList + ", currentPreset=" + preset + ", presetUser=" + presetUser);
        presetsListSize = presetsList.removePreset(index);
        // The first preset is removed
        if (addPresetButton && position == 0) {
            update();
        } else if(preset.equals(presetUser)) {
            update();
        } else {
            adapter.notifyItemRemoved(position);
            notifyItemRangeChanged(position);
        }
        if (position == 0) {
            mainActivity.updatePresetsVisibility();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            mainActivity.updateShortcuts();
        }
    }

    private void removeDuplicatedPreset(int position) {
        int index = getListIndex(position);
        Log.d(TAG, "removeDuplicatedPreset: position=" + position + ", index=" + index + ", presetList=" + presetsList);
        presetsListSize = presetsList.removePreset(index);
        adapter.notifyItemRemoved(position);
        notifyItemRangeChanged(position);
    }

    public void update() {
        Preset currentPreset = mainActivity.getPresetUser();
        if (!currentPreset.equals(presetUser)) {
            presetUser = currentPreset;
        }
        int index = presetsList.indexOf(currentPreset);
        Log.d(TAG, "update: presetUser=" + presetUser + ", index=" + index + ", addPresetButton=" + addPresetButton + ", userPosition=" + userPosition);

        if (index == -1) {
            Log.d(TAG, "update: preset is not in the list, index=" + index);
            // Preset is not in the list
            if (presetUser.isValid()) {
                Log.d(TAG, "update: preset is valid, addPresetButton=" + addPresetButton);
                // Preset is valid
                if (!addPresetButton) {
                    // Add the preset to the list
                    addPresetButton = true;
                    notifyItemRangeChanged(1);
                    scrollToPosition(0);
                    // Remove empty preset list indications
                    mainActivity.updatePresetsVisibility();
                }
                else {
                    // Preset is already selected
                    Log.d(TAG, "update: userPosition=" + userPosition);
                    notifyItemChanged(0);
                    userPosition = 0;
                }
            } else {
                // Preset is invalid, MainActivity layout is WAITING
                if (addPresetButton) {
                    addPresetButton = false;
                    adapter.notifyItemRemoved(0);
                } else {
                    notifyItemChanged(userPosition);
                }
                userPosition = USER_POSITION_NONE;
            }
        } else {
            // Preset already exists in the list
            if (addPresetButton) {
                addPresetButton = false;
                adapter.notifyItemRemoved(0);
                notifyItemRangeChanged(0);
                scrollToPosition(index);
            } else if (userPosition != index) {
                // Update both positions
                notifyItemChanged(userPosition);
                notifyItemChanged(index);
                scrollToPosition(index);
            } else {
                // Preset is already selected
                Log.d(TAG, "update: userPosition=" + userPosition + ", position=" + index);
            }
        }
    }

    ArrayList<Preset> getPresetsList() {
        return presetsList.getPresetsList();
    }

    void updateFromPreferences() {
        if (!presetsList.isSynced()) {
            Log.d(TAG, "updateFromPreferences: presets are not synced");
            int previousPresetsListSize = presetsListSize;
            presetsListSize = presetsList.initPresets();
            if (previousPresetsListSize > presetsListSize) {
                adapter.notifyItemRangeRemoved(presetsListSize, previousPresetsListSize - presetsListSize);
            } else if (presetsListSize > previousPresetsListSize) {
                adapter.notifyItemRangeInserted(previousPresetsListSize, presetsListSize - previousPresetsListSize);
            }
            notifyItemRangeChanged(0);
            scrollToPosition(0);
        }
    }

    private void notifyItemChanged(int position) {
        if (position >= 0 && position < adapter.getItemCount()) {
            adapter.notifyItemChanged(position);
        }
    }

    private void notifyItemRangeChanged(int positionStart) {
        if (positionStart >= 0 && positionStart < adapter.getItemCount()) {
            adapter.notifyItemRangeChanged(positionStart, adapter.getItemCount() - positionStart);
        }
    }

    private int getListIndex(int index) {
        return addPresetButton ? index - 1 : index;
    }

    boolean isEmpty() { return presetsListSize == 0; }

    void setCurrentPresetName(String name) {
        Log.d(TAG, "setCurrentPresetName: userPosition=" + userPosition + ", name=" + name);
        int listIndex = getListIndex(userPosition);
        if (listIndex >= 0 && !removeDuplicate(name)) {
            // duplicates can be created while renaming a timer
            presetsList.updatePresetName(listIndex, name);
        }
        notifyItemChanged(userPosition);
    }

    private void switchDisplayModePreset(int position) {
        presetUser.switchDisplayMode();
        int displayMode = presetUser.getDisplayMode();
        int listIndex = getListIndex(position);
        if (listIndex >= 0) {
            Log.d(TAG, "switchDisplayModePreset: listIndex=" + listIndex + ", displayMode=" + displayMode);
            presetsList.switchPresetDisplayMode(listIndex, displayMode);
        }
        mainActivity.updateDisplayMode(displayMode);
    }

    private boolean removeDuplicate(String name) {
        Preset renamedPreset = mainActivity.getPresetUser();
        renamedPreset.setName(name);
        Log.d(TAG, "removeDuplicate: renamedPreset=" + renamedPreset);
        if (renamedPreset.isValid()) {
            int duplicateIndex = presetsList.find(renamedPreset);
            Log.d(TAG, "removeDuplicate: duplicateIndex=" + duplicateIndex + ", userPosition=" + userPosition);
            if (duplicateIndex != -1 && duplicateIndex != userPosition) {
                removeDuplicatedPreset(userPosition);
                return true;
            }
        } else {
            Log.e(TAG, "invalid renamedPreset=" + renamedPreset);
        }
        return false;
    }

    private void scrollToPosition(int position) {
        Log.d(TAG, "scrollToPosition: position=" + position);
        userPosition = position;
        linearLayoutManager.scrollToPosition(userPosition);
    }

    void initContext(Context context) {
        this.context = context;
        createDeleteAlertDialog();
        createSelectAlertDialog();
    }

    void createPresetsList(SharedPreferences sharedPreferences){
        Log.d(TAG, "createPresetsList");
        this.sharedPreferences = sharedPreferences;
        presetsList = new PresetsList();
        presetsList.setContext(context);
        presetsList.setSharedPreferences(sharedPreferences);
        presetsListSize = presetsList.initPresets();
    }

    private void createDeleteAlertDialog() {
        deleteAlertDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme).create();
        deleteAlertDialog.setMessage(context.getString(R.string.delete_preset));
        deleteAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.alert_no),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
    }

    private void createSelectAlertDialog() {
        selectAlertDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme).create();
        selectAlertDialog.setMessage(context.getString(R.string.select_preset));
        selectAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.alert_no),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        adapter = new RecycleViewAdapter();
        addPresetButton = false;
        userPosition = USER_POSITION_NONE;
        mainActivity = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        linearLayoutManager = new LinearLayoutManager(mainActivity);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        scrollToPosition(USER_POSITION_NONE);

        View view = inflater.inflate(R.layout.fragment_horizontal_preset_cards, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.cardView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        recyclerView.invalidate();
        recyclerView.setLayoutManager(linearLayoutManager);

        ItemTouchHelper.Callback callback = new PresetTouchHelper();
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        return view;
    }

    private class PresetTouchHelper extends ItemTouchHelper.SimpleCallback {

        PresetTouchHelper() {
            super(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            if (source.getItemViewType() != target.getItemViewType()) {
                Log.d(TAG, "onMove: different item view type");
                return true;
            }

            int sourcePosition = source.getAdapterPosition();
            int targetPosition = target.getAdapterPosition();

            if (targetPosition == 0 && addPresetButton) {
                Log.e(TAG, "onMove: targetAdapterPosition=0");
            } else {
                adapter.onItemMove(sourcePosition, targetPosition);
                adapter.notifyItemMoved(sourcePosition, targetPosition);
            }
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.getItemViewType() == ITEM_VIEW_TYPE_PRESET) {
                return makeMovementFlags(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0);
            }
            return 0;
        }
    }


    private class RecycleViewAdapter extends RecyclerView.Adapter {

        private static final int ITEM_VIEW_TYPE_PRESET_ADD = 0;

        @Override
        public int getItemViewType(int position) {
            return (position > 0)? ITEM_VIEW_TYPE_PRESET : addPresetButton ? ITEM_VIEW_TYPE_PRESET_ADD : ITEM_VIEW_TYPE_PRESET;
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            ((SimpleItemAnimator) Objects.requireNonNull(recyclerView.getItemAnimator())).setSupportsChangeAnimations(false);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder: viewType=" + viewType);
            if (viewType == ITEM_VIEW_TYPE_PRESET) {
                return new PresetViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.preset_card_view, parent, false));
            } else {
                return new AddPresetViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.preset_card_view, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int position) {
            if (viewHolder.getItemViewType() == ITEM_VIEW_TYPE_PRESET) {
                Preset preset = presetsList.getPreset(getListIndex(position));
                updateTextViews((BaseViewHolder)viewHolder, preset);
                PresetViewHolder presetViewHolder = (PresetViewHolder)viewHolder;
                presetViewHolder.imageButtonCard.setImageResource(R.drawable.ic_preset_delete);
                if (preset.equals(presetUser)) {
                    Log.d(TAG, "onBindViewHolder: isPresetUser, preset=" + presetsList.getPreset(getListIndex(position)));
                    presetViewHolder.linearLayoutCard.setBackgroundColor(ContextCompat.getColor(context, R.color.preset_card_user_background));
                } else {
                    Log.d(TAG, "onBindViewHolder: isNotPresetUser, preset=" + presetsList.getPreset(getListIndex(position)));
                    presetViewHolder.linearLayoutCard.setBackgroundColor(ContextCompat.getColor(context, R.color.preset_card_background));
                }
                Log.d(TAG, "onBindViewHolder: position=" + position + ", preset=" + presetsList.getPreset(getListIndex(position)));
            } else {
                AddPresetViewHolder addPresetViewHolder = (AddPresetViewHolder)viewHolder;
                addPresetViewHolder.imageButtonCard.setImageResource(R.drawable.ic_preset_add);
                updateTextViews((BaseViewHolder) viewHolder, presetUser);
            }
            Log.d(TAG, "onBindViewHolder: position=" + position + ", presetUser=" + presetUser);
        }

        @Override
        public int getItemCount() {
            return presetsListSize + (addPresetButton? 1 : 0);
        }

        void onItemMove(int fromPosition, int toPosition) {
            int fromListIndex = getListIndex(fromPosition);
            int toListIndex = getListIndex(toPosition);
            if (!addPresetButton) {
                if (presetsList.getPreset(fromListIndex).equals(presetUser)){
                    Log.d(TAG, "onItemMove: fromListIndex=" + fromListIndex + " preset is active");
                    userPosition = toPosition;
                }
                if (presetsList.getPreset(toListIndex).equals(presetUser)){
                    Log.d(TAG, "onItemMove: toListIndex=" + toListIndex + " preset is active");
                    userPosition = fromPosition;
                }
            }
            presetsListSize = presetsList.swapPreset(fromListIndex, toListIndex);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                mainActivity.updateShortcuts();
            }
        }
    }

    private void updateTextViews(BaseViewHolder holder, Preset preset) {
        int displayMode = preset.getDisplayMode();
        if (displayMode == Preset.DISPLAY_MODE_TIMER) {
            final boolean hours = preset.getTimer() >= 3600;
            final int timerHoursVisibility = hours ? View.VISIBLE : View.GONE;
            updateTextViewsVisibility(holder, timerHoursVisibility, View.VISIBLE, View.GONE);
            holder.textViewCardTimerHours.setText(preset.getTimerHoursString());
            holder.textViewCardTimerMinutes.setText(preset.getTimerMinutesString(hours));
            holder.textViewCardTimerSeconds.setText(preset.getTimerSecondsString());
            if (preset.isInfinity()) {
                holder.textViewCardSets.setVisibility(View.GONE);
            } else {
                holder.textViewCardSets.setVisibility(View.VISIBLE);
                holder.textViewCardSets.setText(preset.getSetsString());
            }
        } else if (displayMode == Preset.DISPLAY_MODE_NAME) {
            updateTextViewsVisibility(holder, View.GONE, View.GONE, View.VISIBLE);
            holder.textViewCardSets.setVisibility(View.GONE);
            holder.textViewCardTimerName.setText(preset.getName());
        } else {
            Log.e(TAG, "updateTextViewsVisibility: invalid displayMode=" + displayMode);
        }
    }

    private void updateTextViewsVisibility(BaseViewHolder holder, final int timerHoursVisibility, final int timerVisibility, final int nameVisibility) {
        holder.textViewCardTimerHours.setVisibility(timerHoursVisibility);
        holder.textViewCardTimerSeparatorHours.setVisibility(timerHoursVisibility);

        holder.textViewCardTimerMinutes.setVisibility(timerVisibility);
        holder.textViewCardTimerSeparator.setVisibility(timerVisibility);
        holder.textViewCardTimerSeconds.setVisibility(timerVisibility);
        holder.textViewCardTimerName.setVisibility(nameVisibility);
    }

    private class BaseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView textViewCardTimerHours;
        final TextView textViewCardTimerSeparatorHours;
        final TextView textViewCardTimerMinutes;
        final TextView textViewCardTimerSeparator;
        final TextView textViewCardTimerSeconds;
        final TextView textViewCardTimerName;
        final TextView textViewCardSets;
        final ImageButton imageButtonCard;
        final LinearLayout linearLayoutCard;
        final LinearLayout linearLayoutTimer;

        void changeDisplayMode(int position) {
            switchDisplayModePreset(position);
            notifyItemChanged(position);
        }

        BaseViewHolder(final View view) {
            super(view);
            textViewCardTimerHours = view.findViewById(R.id.textViewCardTimerHours);
            textViewCardTimerSeparatorHours = view.findViewById(R.id.textViewCardTimerSeparatorHours);
            textViewCardTimerMinutes = view.findViewById(R.id.textViewCardTimerMinutes);
            textViewCardTimerSeparator = view.findViewById(R.id.textViewCardTimerSeparator);
            textViewCardTimerSeconds = view.findViewById(R.id.textViewCardTimerSeconds);
            textViewCardTimerName = view.findViewById(R.id.textViewCardTimerName);
            textViewCardSets = view.findViewById(R.id.textViewCardSets);
            imageButtonCard = view.findViewById(R.id.imageButtonCard);
            linearLayoutCard = view.findViewById(R.id.layoutCard);
            linearLayoutTimer = view.findViewById(R.id.layoutCardTimer);

            Typeface typeface = Typeface.createFromAsset(mainActivity.getAssets(), "fonts/Lekton-Bold.ttf");
            Typeface typefaceLight = Typeface.createFromAsset(mainActivity.getAssets(), "fonts/Lekton-Regular.ttf");
            textViewCardTimerHours.setTypeface(typeface);
            textViewCardTimerSeparatorHours.setTypeface(typeface);
            textViewCardTimerMinutes.setTypeface(typeface);
            textViewCardTimerSeparator.setTypeface(typeface);
            textViewCardTimerSeconds.setTypeface(typeface);
            textViewCardTimerName.setTypeface(typeface);
            textViewCardSets.setTypeface(typefaceLight);

            textViewCardTimerHours.setOnClickListener(this);
            textViewCardTimerSeparatorHours.setOnClickListener(this);
            textViewCardTimerMinutes.setOnClickListener(this);
            textViewCardTimerSeparator.setOnClickListener(this);
            textViewCardTimerSeconds.setOnClickListener(this);
            textViewCardTimerName.setOnClickListener(this);
            textViewCardSets.setOnClickListener(this);
            imageButtonCard.setOnClickListener(this);
            linearLayoutCard.setOnClickListener(this);
            linearLayoutTimer.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            final int position = getAdapterPosition();
            if (view.getId() == imageButtonCard.getId()) {
                onClickImageButtonCard(position);
            } else {
                onClickView(position);
            }
        }

        protected void onClickImageButtonCard(final int position) {
            Log.e(TAG, "onClickImageButtonCard: no action position=" + position);
        }

        protected void onClickView(final int position) {
            Log.d(TAG, "onClickView: position=" + position +", userPosition=" + userPosition);
            if (position == userPosition) {
                changeDisplayMode(position);
            }
        }
    }

    private class PresetViewHolder extends BaseViewHolder {

        PresetViewHolder(final View view) {
            super(view);
        }

        void selectPosition(final int position) {
            if (position != userPosition) {
                mainActivity.inputPreset(presetsList.getPreset(getListIndex(position)));
                userPosition = position;
            }
        }

        @Override
        protected void onClickImageButtonCard(final int position) {
            Log.d(TAG, "onClickImageButtonCard: delete preset position=" + position);
            deleteAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.alert_yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            removePreset(position);
                        }
                    });
            deleteAlertDialog.show();
        }

        @Override
        protected void onClickView(final int position) {
            super.onClickView(position);
            final boolean ask = sharedPreferences.getBoolean(context.getString(R.string.pref_preset_select_asking), true);
            Log.d(TAG, "onClickView: ask=" + ask + ", position=" + position);
            if (ask && mainActivity.isCurrentTimerRunning()) {
                selectAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.alert_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                selectPosition(position);
                            }
                        });
                selectAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, context.getString(R.string.alert_stop_asking),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "createSelectAlertDialog: stop asking");
                                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                                sharedPreferencesEditor.putBoolean(context.getString(R.string.pref_preset_select_asking), false);
                                sharedPreferencesEditor.apply();
                                selectPosition(position);
                            }
                        });
                selectAlertDialog.show();
            } else {
                selectPosition(position);
            }
        }
    }

    private class AddPresetViewHolder extends BaseViewHolder implements View.OnClickListener {

        AddPresetViewHolder(final View view) {
            super(view);
        }

        @Override
        protected void onClickImageButtonCard(final int position) {
            Log.d(TAG, "onClickImageButtonCard: add preset");
            addPreset();
        }
    }
}


