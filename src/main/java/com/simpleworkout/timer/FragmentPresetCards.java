package com.simpleworkout.timer;

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

public class FragmentPresetCards extends Fragment {

    private static final String TAG = "FragmentPresetCards";

    LinearLayoutManager linearLayoutManager;
    RecyclerView recyclerView;
    private RecycleViewAdapter adapter;

    private ArrayList<Preset> presetCards = new ArrayList<>();

    public void addPresetCard(int position, Preset preset) {
        Log.d(TAG, "addPresetCard: preset='" + preset.toString() + "'");
        presetCards.add(position, preset);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void deletePresetCard(int position) {
        presetCards.remove(position);
        if (adapter != null) {
            adapter.notifyItemRemoved(position + 1);
            adapter.notifyItemRangeChanged(position + 1, adapter.getItemCount());
        }
    }

    public int getPresetCount() {
        return presetCards.size();
    }

    public boolean presetCardExists(Preset preset) {
        return presetCards.contains(preset);
    }

    public void showPresetCard(Preset preset) {
        int position = presetCards.indexOf(preset);
        linearLayoutManager.scrollToPosition(position + 1);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        adapter = new RecycleViewAdapter();
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
                presetViewHolder.textViewCardTimer.setText(presetCards.get(position - 1).getTimerString());
                presetViewHolder.textViewCardSets.setText(presetCards.get(position - 1).getSetsString());
                Log.d(TAG, "onBindViewHolder: position=" + position);
            }
        }

        @Override
        public int getItemCount() {
            return presetCards.size() + 1;
        }

        void onItemMove(int fromPosition, int toPosition) {
            fromPosition -= 1;
            toPosition -= 1;
            Log.d(TAG, "onItemMove: fromPosition=" + fromPosition + ", toPosition=" + toPosition);
            Log.d(TAG, "onItemMove: before=" + presetCards.toString());
            Preset preset = presetCards.get(fromPosition);
            presetCards.remove(fromPosition);
            presetCards.add(toPosition, preset);
            Log.d(TAG, "onItemMove: after=" + presetCards.toString());
            ((MainActivity)getActivity()).movePreset(fromPosition, toPosition);
        }
    }

    private class PresetViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView textViewCardTimer, textViewCardSets;
        private ImageButton imageButtonCard;

        private void inputPreset(int position) {
            ((MainActivity)getActivity()).inputPreset(position);
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
                ((MainActivity)getActivity()).removePreset(position);
                deletePresetCard(position);
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
}


