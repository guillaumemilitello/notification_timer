package com.simpleworkout.timer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class FragmentPresetCards extends Fragment {

    private static final String TAG = "FragmentPresetCards";

    private PresetList presetsList;

    LinearLayoutManager linearLayoutManager;
    RecyclerView recyclerView;
    private RecycleViewAdapter adapter;
    private boolean addPresetButton;

    public boolean addPreset(Preset preset) {
        Log.d(TAG, "addPreset: preset='" + preset.toString() + "'");
        int index = presetsList.indexOf(preset);
        if (index == -1) {
            presetsList.addPreset(0, preset);
            disableAddPresetButton();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            return true;
        }
        linearLayoutManager.scrollToPosition(index + 1);
        return false;
    }

    private void removePreset(int position) {
        presetsList.removePreset(position);
        if (adapter != null) {
            adapter.notifyItemRemoved(position + 1);
            adapter.notifyItemRangeChanged(position + 1, adapter.getItemCount());
        }
    }

    public void disableAddPresetButton() {
        if (adapter != null && addPresetButton) {
            addPresetButton = false;
            adapter.notifyItemChanged(0);
        }
    }

    private void enableAddPresetButton() {
        if (adapter != null && !addPresetButton) {
            addPresetButton = true;
            adapter.notifyItemChanged(0);
        }
    }

    public void updateAddPresetCard(Preset preset) {
        int index = presetsList.indexOf(preset);
        if (index == -1) {
            enableAddPresetButton();
            linearLayoutManager.scrollToPosition(0);
        } else {
            disableAddPresetButton();
            linearLayoutManager.scrollToPosition(index + 1);
        }
    }

    public void createPresetsList(Context context, SharedPreferences sharedPreferences){
        presetsList = new PresetList();
        presetsList.context = context;
        presetsList.sharedPreferences = sharedPreferences;
        presetsList.initPresets();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        adapter = new RecycleViewAdapter();
        addPresetButton = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        View view = inflater.inflate(R.layout.fragment_horizontal_preset_cards, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.cardView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        recyclerView.invalidate();
        recyclerView.setLayoutManager(linearLayoutManager);

        ItemTouchHelper.Callback callback = new PresetTouchHelper();
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private class PresetTouchHelper extends ItemTouchHelper.SimpleCallback {

        PresetTouchHelper() {
            super(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            if (source.getItemViewType() != target.getItemViewType()) {
                Log.d(TAG, "onMove: different item view type");
                return true;
            }

            int sourcePosition = source.getAdapterPosition();
            int targetPosition = target.getAdapterPosition();

            if (targetPosition == 0) {
                Log.e(TAG, "onMove: targetAdapterPosition=0");
            } else {
                adapter.onItemMove(sourcePosition, targetPosition);
                adapter.notifyItemMoved(sourcePosition, targetPosition);
            }
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
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
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            return makeMovementFlags(dragFlags, 0);
        }
    }


    public class RecycleViewAdapter extends RecyclerView.Adapter {

        @Override
        public int getItemViewType(int position) {
            return (position != 0)? 1 : 0;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            if (viewType == 1) {
                return new PresetViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.preset_card_view, parent, false));
            } else {
                return new AddPresetViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.preset_add_card_view, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 1) {
                PresetViewHolder presetViewHolder = (PresetViewHolder)holder;
                presetViewHolder.textViewCardTimer.setText(presetsList.getPreset(position - 1).getTimerString());
                presetViewHolder.textViewCardSets.setText(presetsList.getPreset(position - 1).getSetsString());
                Log.d(TAG, "onBindViewHolder: position=" + position);
            } else {
                AddPresetViewHolder addPresetViewHolder = (AddPresetViewHolder)holder;
                addPresetViewHolder.imageButtonCard.setAlpha(addPresetButton? 1.f : MainActivity.ALPHA_DISABLED);
                Log.d(TAG, "onBindViewHolder: position=" + position);
            }
        }

        @Override
        public int getItemCount() {
            return presetsList.getSize() + 1;
        }

        void onItemMove(int fromPosition, int toPosition) {
            fromPosition -= 1;
            toPosition -= 1;
            Log.d(TAG, "onItemMove: fromPosition=" + fromPosition + ", toPosition=" + toPosition);
            presetsList.swapPreset(fromPosition, toPosition);
        }
    }

    private class PresetViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView textViewCardTimer, textViewCardSets;
        private ImageButton imageButtonCard;

        private void inputPreset(int position) {
            ((MainActivity)getActivity()).inputPreset(presetsList.getPreset(position));
        }

        PresetViewHolder(final View view) {
            super(view);
            textViewCardTimer = (TextView) view.findViewById(R.id.textViewCardTimer);
            textViewCardSets = (TextView) view.findViewById(R.id.textViewCardSets);
            imageButtonCard = (ImageButton) view.findViewById(R.id.imageButtonCard);

            textViewCardTimer.setOnClickListener(this);
            textViewCardSets.setOnClickListener(this);
            imageButtonCard.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            if (view.getId() == textViewCardTimer.getId() || view.getId() == textViewCardSets.getId()){
                Log.d(TAG, "selectPreset: position=" + (getAdapterPosition() - 1));
                inputPreset(getAdapterPosition() - 1);
            }
            else if (view.getId() == imageButtonCard.getId()) {
                int position = getAdapterPosition() - 1;
                Log.d(TAG, "setOnClick: delete preset position=" + position);
                removePreset(getAdapterPosition() - 1);
                enableAddPresetButton();
            }
        }
    }

    private class AddPresetViewHolder extends RecyclerView.ViewHolder {

        private ImageButton imageButtonCard;

        AddPresetViewHolder(View view) {
            super(view);
            imageButtonCard = (ImageButton) view.findViewById(R.id.imageButtonAddCard);

            imageButtonCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "OnClick: add preset");
                    ((MainActivity)getActivity()).addPreset();
                }
            });
        }
    }

    private class PresetList {

        private ArrayList<Preset> list = new ArrayList<>();

        SharedPreferences sharedPreferences;
        Context context;

        void addPreset(int index, Preset preset) {
            Log.d(TAG, "addPreset: index=" + index + ", preset='" + preset.toString() + "'");
            list.add(index, preset);
            for (int p = getSize(); p > index; --p) {
                savePreset(p, loadPreset(p - 1));
            }
            savePreset(index, preset);
        }

        void removePreset(int index) {
            Log.d(TAG, "removePreset: index=" + index);
            list.remove(index);
            for (int p = index; p < getSize(); ++p) {
                savePreset(p, loadPreset(p + 1));
            }
            erasePreset(getSize());
        }

        void swapPreset(int fromIndex, int toIndex) {
            Log.d(TAG, "movePreset: fromIndex=" + fromIndex + ", toIndex=" + toIndex);
            Collections.swap(list, fromIndex, toIndex);
            Preset preset = loadPreset(fromIndex);
            removePreset(fromIndex);
            addPreset(toIndex, preset);
        }

        int indexOf(Preset preset) {
            return list.indexOf(preset);
        }

        Preset getPreset(int index) {
            return list.get(index);
        }

        private int getSize() {
            return list.size();
        }

        private void savePreset(int index, final Preset preset) {
            if (sharedPreferences != null) {
                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putLong(String.format(Locale.US, context.getString(R.string.pref_preset_array_timer), index), preset.getTimer());
                sharedPreferencesEditor.putInt(String.format(Locale.US, context.getString(R.string.pref_preset_array_sets), index), preset.getSets());
                sharedPreferencesEditor.putInt(String.format(Locale.US, context.getString(R.string.pref_preset_array_init), index), preset.getInit());
                sharedPreferencesEditor.apply();
                Log.d(TAG, "savePreset: index=" + index + ", preset='" + preset + "'");
            }
        }

        private Preset loadPreset(int index) {
            if (sharedPreferences != null) {
                long timer = sharedPreferences.getLong(String.format(Locale.US, context.getString(R.string.pref_preset_array_timer), index), -1);
                int sets = sharedPreferences.getInt(String.format(Locale.US, context.getString(R.string.pref_preset_array_sets), index), -1);
                int init = sharedPreferences.getInt(String.format(Locale.US, context.getString(R.string.pref_preset_array_init), index), -1);
                Preset preset = new Preset(timer, sets, init);
                Log.d(TAG, "loadPreset: index=" + index + ", preset='" + preset + "'");
                return preset;
            } else {
                return new Preset();
            }
        }

        private void erasePreset(int index) {
            savePreset(index, new Preset());
        }

        private void initPresets() {
            Log.d(TAG, "initPresets");
            int index = 0;
            while (true) {
                Preset preset = loadPreset(index);
                if (preset.isValid()) {
                    list.add(index++, preset);
                } else {
                    break;
                }
            }
        }
    }
}


