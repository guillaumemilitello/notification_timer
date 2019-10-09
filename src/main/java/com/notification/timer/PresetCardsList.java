package com.notification.timer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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

import java.util.Objects;

public class PresetCardsList extends Fragment {

    private static final String TAG = "PresetCardsList";

    private MainActivity mainActivity;

    private PresetsList presetsList;
    private int presetsListSize = 0;

    private Preset presetUser;

    private static final int USER_POSITION_NONE = -1;
    private int userPosition;

    private LinearLayoutManager linearLayoutManager;
    private RecycleViewAdapter adapter;
    private boolean addPresetButton;

    private Context context;
    private AlertDialog alertDialog;

    void addPreset() {
        if (presetsList.indexOf(presetUser) == -1) {
            presetsListSize = presetsList.addPreset(0, presetUser);
            Log.d(TAG, "addPreset: presetUser=" + presetUser + ", presetList=" + presetsList);
            addPresetButton = false;
            notifyItemChanged(0);
            scrollToPosition(0);
        } else {
            Log.e(TAG, "addPreset: presetUser=" + presetUser + ", presetList=" + presetsList + " already exists, index=" + presetsList.indexOf(presetUser));
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
                    userPosition = USER_POSITION_NONE;
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
                userPosition = index;
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

    private void scrollToPosition(int position) {
        Log.d(TAG, "scrollToPosition: position=" + position);
        userPosition = position;
        linearLayoutManager.scrollToPosition(userPosition);
    }

    void initContext(Context context) {
        this.context = context;
        createAlertDialog();
    }

    void createPresetsList(SharedPreferences sharedPreferences){
        Log.d(TAG, "createPresetsList");
        presetsList = new PresetsList();
        presetsList.setContext(context);
        presetsList.setSharedPreferences(sharedPreferences);
        presetsListSize = presetsList.initPresets();
    }

    private void createAlertDialog() {
        alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme).create();
        alertDialog.setMessage(context.getString(R.string.delete_preset));
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.alert_no),
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
            int dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            return makeMovementFlags(dragFlags, 0);
        }
    }


    private class RecycleViewAdapter extends RecyclerView.Adapter {

        private static final int ITEM_VIEW_TYPE_PRESET_ADD = 0;
        private static final int ITEM_VIEW_TYPE_PRESET = 1;

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
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == ITEM_VIEW_TYPE_PRESET) {
                PresetViewHolder presetViewHolder = (PresetViewHolder)holder;
                presetViewHolder.imageButtonCard.setImageResource(R.drawable.ic_preset_delete);
                Preset preset = presetsList.getPreset(getListIndex(position));
                boolean hours = preset.getTimer() >= 3600;
                presetViewHolder.textViewCardTimerHours.setVisibility(hours ? View.VISIBLE : View.GONE);
                presetViewHolder.textViewCardTimerSeparatorHours.setVisibility(hours ? View.VISIBLE : View.GONE);
                presetViewHolder.textViewCardTimerHours.setText(preset.getTimerHoursString());
                presetViewHolder.textViewCardTimerMinutes.setText(preset.getTimerMinutesString(hours));
                presetViewHolder.textViewCardTimerSeconds.setText(preset.getTimerSecondsString());
                if (preset.isInfinity()) {
                    presetViewHolder.textViewCardSets.setVisibility(View.GONE);
                } else {
                    presetViewHolder.textViewCardSets.setVisibility(View.VISIBLE);
                    presetViewHolder.textViewCardSets.setText(preset.getSetsString());
                }
                if (preset.equals(presetUser)) {
                    Log.d(TAG, "onBindViewHolder: isPresetUser, preset=" + presetsList.getPreset(getListIndex(position)));
                    presetViewHolder.linearLayoutCard.setBackgroundColor(ContextCompat.getColor(context, R.color.preset_card_user_background));
                } else {
                    Log.d(TAG, "onBindViewHolder: isNotPresetUser, preset=" + presetsList.getPreset(getListIndex(position)));
                    presetViewHolder.linearLayoutCard.setBackgroundColor(ContextCompat.getColor(context, R.color.preset_card_background));
                }
                Log.d(TAG, "onBindViewHolder: position=" + position + ", preset=" + presetsList.getPreset(getListIndex(position)));
            } else {
                AddPresetViewHolder addPresetViewHolder = (AddPresetViewHolder)holder;
                addPresetViewHolder.imageButtonCard.setImageResource(R.drawable.ic_preset_add);
                boolean hours = presetUser.getTimer() >= 3600;
                addPresetViewHolder.textViewCardTimerHours.setVisibility(hours ? View.VISIBLE : View.GONE);
                addPresetViewHolder.textViewCardTimerSeparatorHours.setVisibility(hours ? View.VISIBLE : View.GONE);
                addPresetViewHolder.textViewCardTimerHours.setText(presetUser.getTimerHoursString());
                addPresetViewHolder.textViewCardTimerMinutes.setText(presetUser.getTimerMinutesString(hours));
                addPresetViewHolder.textViewCardTimerSeconds.setText(presetUser.getTimerSecondsString());
                if (presetUser.isInfinity()) {
                    addPresetViewHolder.textViewCardSets.setVisibility(View.GONE);
                } else {
                    addPresetViewHolder.textViewCardSets.setVisibility(View.VISIBLE);
                    addPresetViewHolder.textViewCardSets.setText(presetUser.getSetsString());
                }
                Log.d(TAG, "onBindViewHolder: position=" + position + ", presetUser=" + presetUser);
            }
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
        }
    }

    private class PresetViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView textViewCardTimerHours;
        private final TextView textViewCardTimerSeparatorHours;
        private final TextView textViewCardTimerMinutes;
        private final TextView textViewCardTimerSeconds;
        private final TextView textViewCardSets;
        private final ImageButton imageButtonCard;
        private final LinearLayout linearLayoutCard;

        private void inputPreset(int position) {
            mainActivity.inputPreset(presetsList.getPreset(getListIndex(position)));
        }

        PresetViewHolder(final View view) {
            super(view);
            textViewCardTimerHours = view.findViewById(R.id.textViewCardTimerHours);
            textViewCardTimerSeparatorHours = view.findViewById(R.id.textViewCardTimerSeparatorHours);
            textViewCardTimerMinutes = view.findViewById(R.id.textViewCardTimerMinutes);
            final TextView textViewCardTimerSeparator = view.findViewById(R.id.textViewCardTimerSeparator);
            textViewCardTimerSeconds = view.findViewById(R.id.textViewCardTimerSeconds);
            textViewCardSets = view.findViewById(R.id.textViewCardSets);
            imageButtonCard = view.findViewById(R.id.imageButtonCard);
            linearLayoutCard = view.findViewById(R.id.layoutCard);
            final LinearLayout linearLayoutTimer = view.findViewById(R.id.layoutCardTimer);

            Typeface typeface = Typeface.createFromAsset(mainActivity.getAssets(), "fonts/Lekton-Bold.ttf");
            Typeface typefaceLight = Typeface.createFromAsset(mainActivity.getAssets(), "fonts/Lekton-Regular.ttf");
            textViewCardTimerHours.setTypeface(typeface);
            textViewCardTimerSeparatorHours.setTypeface(typeface);
            textViewCardTimerMinutes.setTypeface(typeface);
            textViewCardTimerSeparator.setTypeface(typeface);
            textViewCardTimerSeconds.setTypeface(typeface);
            textViewCardSets.setTypeface(typefaceLight);

            linearLayoutTimer.setOnClickListener(this);
            textViewCardTimerHours.setOnClickListener(this);
            textViewCardTimerSeparatorHours.setOnClickListener(this);
            textViewCardTimerMinutes.setOnClickListener(this);
            textViewCardTimerSeparator.setOnClickListener(this);
            textViewCardTimerSeconds.setOnClickListener(this);
            textViewCardSets.setOnClickListener(this);
            imageButtonCard.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == imageButtonCard.getId()) {
                final int position = getAdapterPosition();
                Log.d(TAG, "onClick: delete preset position=" + position);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.alert_yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            removePreset(position);
                        }
                    });
                alertDialog.show();
            } else {
                int position = getAdapterPosition();
                Log.d(TAG, "onClick: position=" + position +", userPosition=" + userPosition);
                if (position != userPosition) {
                    inputPreset(getAdapterPosition());
                }
            }
        }
    }

    private class AddPresetViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewCardTimerHours;
        private final TextView textViewCardTimerSeparatorHours;
        private final TextView textViewCardTimerMinutes;
        private final TextView textViewCardTimerSeconds;
        private final TextView textViewCardSets;
        private final ImageButton imageButtonCard;

        AddPresetViewHolder(final View view) {
            super(view);
            textViewCardTimerHours = view.findViewById(R.id.textViewCardTimerHours);
            textViewCardTimerSeparatorHours = view.findViewById(R.id.textViewCardTimerSeparatorHours);
            textViewCardTimerMinutes = view.findViewById(R.id.textViewCardTimerMinutes);
            final TextView textViewCardTimerSeparator = view.findViewById(R.id.textViewCardTimerSeparator);
            textViewCardTimerSeconds = view.findViewById(R.id.textViewCardTimerSeconds);
            textViewCardSets = view.findViewById(R.id.textViewCardSets);
            imageButtonCard = view.findViewById(R.id.imageButtonCard);

            Typeface typeface = Typeface.createFromAsset(mainActivity.getAssets(), "fonts/Lekton-Bold.ttf");
            Typeface typefaceLight = Typeface.createFromAsset(mainActivity.getAssets(), "fonts/Lekton-Regular.ttf");
            textViewCardTimerHours.setTypeface(typeface);
            textViewCardTimerSeparatorHours.setTypeface(typeface);
            textViewCardTimerMinutes.setTypeface(typeface);
            textViewCardTimerSeparator.setTypeface(typeface);
            textViewCardTimerSeconds.setTypeface(typeface);
            textViewCardSets.setTypeface(typefaceLight);

            imageButtonCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "OnClick: add preset");
                    mainActivity.addPreset();
                }
            });
        }
    }
}


