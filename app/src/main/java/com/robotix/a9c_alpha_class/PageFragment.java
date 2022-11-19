package com.robotix.a9c_alpha_class;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.Html;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.robotix.a9c_alpha_class.BaseData.*;
import static com.robotix.a9c_alpha_class.MainActivity.context;

import static com.robotix.a9c_alpha_class.MainActivity.selectedDate;
import static java.time.temporal.ChronoUnit.DAYS;

public class PageFragment extends Fragment {

    private static final int PROFILE_TAB = 1;
    private static final int DUTY_TAB = 2;
    private static final int OLYMPIAD_TAB = 0;
    private static final int OGE_TAB = 3;
    private static final int EVENTS_TAB = 4;
    private static final int UPDATE_HISTORY_TAB = 5;

    public static final String ARG_PAGE = "ARG_PAGE";

    private SimpleExpandableListAdapter mAdapter;
    ExpandableListView scheduleListView;
    ExpandableListView eventListView;

    private int mPage;

    public static PageFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        PageFragment fragment = new PageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPage = getArguments().getInt(ARG_PAGE);
        }
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page, container, false);
        try {

            TextView textView = (TextView) view.findViewById(R.id.text_in_fragment);
            TextView olympView;
            String[] labels;
            LocalDate date = LocalDate.now();
            long day_number = DAYS.between(LocalDate.of(date.getYear() - 1, 12, 31), date);
            StringBuilder text;
            StringBuilder scheduleText = new StringBuilder();
            SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
            switch (mPage) {

                case PROFILE_TAB: {
                    view = inflater.inflate(R.layout.profile_fragment, container, false);
                    textView = (TextView) view.findViewById(R.id.textView2);

                    Spinner spinner = view.findViewById(R.id.spinner);
                    // Создаем адаптер ArrayAdapter с помощью массива строк и стандартной разметки элемета spinner
                    List<String> list = new LinkedList<>(Arrays.asList(students));
                    list.add(0, "Не выбрано");
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_item, list);
                    // Определяем разметку для использования при выборе элемента
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    // Применяем адаптер к элементу spinner
                    spinner.setAdapter(adapter);

                    spinner.setSelection(settings.getInt("uid", 0));
                    // Добавляем обработчики событий
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putInt("uid", position);
                            editor.apply();
                            NotificationHelper notificationHelper = new NotificationHelper(context);
                            notificationHelper.createNotification();
                            ((SimpleExpandableListAdapter) ((ExpandableListView) parent.getRootView().findViewById(R.id.scheduleExpandableListView)).getExpandableListAdapter()).notifyDataSetChanged();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });


                    // Выбор даты
                    // Текст даты
                    ((TextView) view.findViewById(R.id.date)).setText(date.toString());

                    // Предыдущая неделя
                    AppCompatButton previous_btn = view.findViewById(R.id.previous_week);
                    previous_btn.setOnClickListener(v -> {
                        LocalDate d = LocalDate.parse(selectedDate).minusDays(7);
                        LocalDate monday = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
                        selectedDate = d.toString();
                        Integer[] opened = new Integer[]{};
                        // блокирование кнопки перелистывания назад если это уже прошлое
                        if (d.minusDays(7).isBefore(monday)) v.setEnabled(false);
                        if (d.toString().equals(monday.toString()))
                            opened = new Integer[]{LocalDate.now().getDayOfWeek().getValue() - 1};
                        ((TextView) ((View) v.getParent()).findViewById(R.id.date)).setText(selectedDate);
                        render_schedule((View) v.getRootView(), day_of_week_names, cast_scheduleData_to_renderData(generate_schedule(d)), opened);
                    });

                    // Следующая неделя
                    AppCompatButton next_btn = view.findViewById(R.id.next_week);
                    next_btn.setOnClickListener(v -> {
                        LocalDate d = LocalDate.parse(selectedDate).plusDays(7);
                        selectedDate = d.toString();
                        Integer[] opened = new Integer[]{};
                        LocalDate monday = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
                        // разблокирование кнопки перелистывания назад если это уже не прошлое
                        if (!d.minusDays(7).isBefore(monday))
                            ((View) v.getParent()).findViewById(R.id.previous_week).setEnabled(true);
                        if (d.toString().equals(monday.toString()))
                            opened = new Integer[]{LocalDate.now().getDayOfWeek().getValue() - 1};
                        ((TextView) ((View) v.getParent()).findViewById(R.id.date)).setText(selectedDate);
                        render_schedule((View) v.getRootView(), day_of_week_names, cast_scheduleData_to_renderData(generate_schedule(d)), opened);
                    });

                    // Создание и рендер расписания
                    render_schedule(view, day_of_week_names, cast_scheduleData_to_renderData(generate_schedule(LocalDate.now())), new Integer[]{date.getDayOfWeek().getValue() - 1});

                    text = new StringBuilder();
                    textView.setText(text.toString());
                    break;
                }

                case DUTY_TAB: {
                    text = new StringBuilder("Сегодня (" + date.toString() + ") дежурят:\n" + students[dezhurstva[Integer.parseInt(String.valueOf(day_number)) % (dezhurstva.length / 2) * 2]] + " и " +
                            students[dezhurstva[Integer.parseInt(String.valueOf(day_number)) % (dezhurstva.length / 2) * 2 + 1]]);
                    textView.setText(text.toString());
                    break;
                }

                /*case OLYMPIAD_TAB: {
                    ArrayMap<String, Integer[]> dates = olympDates();
                    view = inflater.inflate(R.layout.olymp_fragment, container, false);
                    textView = (TextView) view.findViewById(R.id.text_olymp);
                    olympView = (TextView) view.findViewById(R.id.text_olymp2);

                    text = "";
                    labels = new String[]{
                            "Сегодня", "Завтра"
                    };
                    for (int k = 0; k < labels.length; k += 1) {
                        text += "<b>" + labels[k] + ":</b> ";
                        if (!dates.keySet().contains(date.plusDays(k).toString())) {
                            text += "нет олимпиад<br><br>";
                        } else {
                            Integer[] olymp_in_day = dates.get(date.plusDays(k).toString());
                            for (int i = 0; i < olymp_in_day.length; i++) {
                                if (studentsOnOlymp[olymp_in_day[i]].length == 0) {
                                    text += "Олимпиада: " + subjects[olymp_in_day[i]] + ". На неё никто не идёт.<br><br>";
                                } else {
                                    text += "Олимпиада: " + subjects[olymp_in_day[i]] + ". На неё идут ";
                                    if (studentsOnOlymp[olymp_in_day[i]].length > 0 && studentsOnOlymp[olymp_in_day[i]][0] == -1) {
                                        text += "все.<br><br>";
                                    } else {
                                        text += ":<br>";
                                        for (int j = 0; j < studentsOnOlymp[olymp_in_day[i]].length; j++) {
                                            text += students[studentsOnOlymp[olymp_in_day[i]][j]] + "<br>";
                                        }
                                        text += "<br>";
                                    }
                                }
                            }
                            text += "<br>";
                        }
                    }
                    for (int i = 0; i < dates.size(); i++) {
                        if (!date.isAfter(LocalDate.parse(dates.keyAt(i)))) {
                            scheduleText += "<b>" + dates.keyAt(i) + "</b>: ";
                            for (int j = 0; j < dates.valueAt(i).length; j++) {
                                scheduleText += subjects[dates.valueAt(i)[j]];
                                if (j < dates.valueAt(i).length - 1) {
                                    scheduleText += ", ";
                                } else {
                                    scheduleText += "<br>";
                                }
                            }
                        }
                    }
                    textView.setText(Html.fromHtml(text));
                    olympView.setText(Html.fromHtml(scheduleText));
                    break;
                }*/

                case OGE_TAB: {
                    view = inflater.inflate(R.layout.olymp_fragment, container, false);
                    textView = view.findViewById(R.id.text_olymp);
                    olympView = view.findViewById(R.id.text_olymp2);

                    labels = new String[]{
                            "Сегодня", "Завтра"
                    };

                    text = new StringBuilder();

                    for (int k = 0; k < labels.length; k++) {
                        DayOfWeek dow = date.plusDays(k).getDayOfWeek();
                        Object[][] OGEs_in_day = OGEs[dow.getValue() - 1];
                        if (OGEs_in_day.length == 0) {
                            text.append("<b>").append(labels[k]).append(" нет консультаций</b><br><br>");
                        }
                        for (Object[] oge : OGEs_in_day) {
                            text.append("<b>")
                                    .append(labels[k])
                                    .append(" консультация:</b> ")
                                    .append(subjects[Integer.parseInt(oge[2].toString().substring(0, oge[2].toString().length() - 1))])
                                    .append(" в ")
                                    .append(get_time_as_string((Integer) oge[0]))
                                    .append(". На неё идут");
                            for (int j = 0; j < OGE_students().get(oge[2]).length; j++) {
                                if (OGE_students().get(oge[2])[j] == -1) {
                                    text.append(" все.<br>");
                                } else {
                                    if (j == 0) {
                                        text.append(":<br>");
                                    }
                                    text.append(students[OGE_students().get(oge[2])[j]]).append("<br>");
                                }
                            }
                            text.append("<br>");
                        }
                    }

                    for (int i = 0; i < 7; i++) {
                        scheduleText.append("<b>").append(day_of_week_names[i]).append(":</b><br>");
                        Object[][] OGEs_in_day = OGEs[i];
                        for (Object[] oge : OGEs_in_day) {
                            scheduleText.append(get_time_as_string((Integer) oge[0])).append(" ").append(subjects[Integer.parseInt(oge[2].toString().substring(0, oge[2].toString().length() - 1))]).append("<br>");
                        }
                        scheduleText.append("<br>");
                    }
                    textView.setText(Html.fromHtml(text.toString()));
                    olympView.setText(Html.fromHtml(scheduleText.toString()));
                    break;
                }

                case EVENTS_TAB: {
                    view = inflater.inflate(R.layout.events_fragment, container, false);
                    render_events(view, getFullEventsArray(), new Integer[]{});
                    break;
                }

                case UPDATE_HISTORY_TAB: {
                    textView.setText(Html.fromHtml(
                            version_history));
                    break;
                }

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return view;
    }

    protected Object[][][] generate_schedule(LocalDate d) {
        Object[][][] schedule = new Object[7][][];
        LocalDate date = d.minusDays(d.getDayOfWeek().getValue() - 1);
        for (int k = 0; k < 7; k++) {
            SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
            SharedPreferences serverEventStorage = context.getSharedPreferences(MainActivity.SERVER_EVENTS_DATA, Context.MODE_PRIVATE);
            Map<String, String> serverEvents = (Map<String, String>) serverEventStorage.getAll();
            int uid = settings.getInt("uid", 0) - 1;
            Object[] adding;

            List<Object[]> day_schedule = new ArrayList<>(Arrays.asList(base_schedule[k]));
            for (Object[] OGE : OGEs[k]) {
                if (Arrays.asList(OGE_students().get(OGE[2])).contains(uid) || OGE_students().get(OGE[2])[0] == -1) {
                    adding = OGE.clone();
                    adding[2] = "Консультация: " + subjects[Integer.parseInt(OGE[2].toString().substring(0, OGE[2].toString().length() - 1))];
                    day_schedule.add(adding);
                }
            }
            for (String key : serverEvents.keySet()) {
                try {
                    MainActivity.checkForOutDated();
                    ArrayList<Object> event = (ArrayList<Object>) ObjectSerializer.deserialize(serverEvents.get(key));
                    if (event.get(0).equals(date.toString()) && (event.get(5).toString().replaceAll(" ", "").equals("_") || Arrays.asList(event.get(5).toString().replaceAll(" ", "").split("-")).contains(Integer.toString(uid)))) {
                        day_schedule.add(new Object[] {Integer.parseInt(event.get(1).toString()), Integer.parseInt(event.get(2).toString()), event.get(3), Integer.parseInt(event.get(4).toString())});
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            day_schedule.sort((obj1, obj2) -> (
                    (Integer) obj1[0]).compareTo((Integer) obj2[0])
            );
            schedule[k] = day_schedule.toArray(new Object[0][]);
            date = date.plusDays(1);
        }

        return schedule;
    }

    public String[][][] cast_scheduleData_to_renderData(Object[][][] schedule) {
        String[][][] schedule_for_render = new String[schedule.length][][];
        for (int i = 0; i < schedule.length; i++) {
            Object[][] day_schedule = schedule[i];
            schedule_for_render[i] = new String[day_schedule.length][];
            for (int j = 0; j < day_schedule.length; j++) {
                schedule_for_render[i][j] = new String[] {
                        get_time_as_string((Integer) day_schedule[j][0]) + "-" + get_time_as_string((Integer) day_schedule[j][0] + (Integer) day_schedule[j][1]),
                        (String) day_schedule[j][2]
                };
            }
        }
        return schedule_for_render;
    }

    protected String[][] getFullEventsArray() {
        ArrayList<String[]> events = new ArrayList<>();
        MainActivity.checkForOutDated();
        SharedPreferences serverEventStorage = context.getSharedPreferences(MainActivity.SERVER_EVENTS_DATA, Context.MODE_PRIVATE);
        Map<String, String> serverEvents = (Map<String, String>) serverEventStorage.getAll();
        String uid = String.valueOf(context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE).getInt("uid", 0) - 1);
        for (String key : serverEvents.keySet()) {
            try {
                ArrayList<Object> event = (ArrayList<Object>) ObjectSerializer.deserialize(serverEvents.get(key));
                if (event.get(5).toString().replaceAll(" ", "").split("-").length == 0 || Arrays.asList(event.get(5).toString().replaceAll(" ", "").split("-")).contains(uid) || uid.equals("-1")) {
                    events.add(new String[]{event.get(0).toString(), event.get(1).toString(), event.get(2).toString(), event.get(3).toString(), event.get(4).toString(), event.get(5).toString()});
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        events.sort((lhs, rhs) -> lhs[0].compareTo(rhs[0]) == 0 ? lhs[1].compareTo(rhs[1]) : lhs[0].compareTo(rhs[0]));

        return events.toArray(new String[][] {});
    }

    public void render_schedule(View view, String[] groupItems, String[][][] childItems, Integer[] opened) {

        String NAME = "NAME";
        String TIME = "TIME";
        scheduleListView = (ExpandableListView) view.findViewById(R.id.scheduleExpandableListView);
        // create lists for group and child items
        List<Map<String, String>> groupData = new ArrayList<>();
        List<List<Map<String, String>>> childData = new ArrayList<>();
        // add data in group and child list
        for (int i = 0; i < groupItems.length; i++) {
            Map<String, String> curGroupMap = new HashMap<>();
            groupData.add(curGroupMap);
            curGroupMap.put(NAME, groupItems[i]);

            List<Map<String, String>> children = new ArrayList<>();
            for (int j = 0; j < childItems[i].length; j++) {
                Map<String, String> curChildMap = new HashMap<>();
                children.add(curChildMap);
                curChildMap.put(TIME, childItems[i][j][0]);
                curChildMap.put(NAME, childItems[i][j][1]);
            }
            childData.add(children);
        }
        // define arrays for displaying data in Expandable list view
        String[] groupFrom = {NAME};
        int[] groupTo = {R.id.heading};
        String[] childFrom = {TIME, NAME};
        int[] childTo = {R.id.childItemTime, R.id.childItemName};


        // Set up the adapter
        mAdapter = new SimpleExpandableListAdapter(context, groupData,
                R.layout.raspisanie_group,
                groupFrom, groupTo,
                childData, R.layout.raspisanie_item,
                childFrom, childTo);

        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                ArrayList<Integer> opened = new ArrayList<>();
                for (int group = 0; group < mAdapter.getGroupCount(); group++) {
                    if (scheduleListView.isGroupExpanded(group)) {
                        opened.add(group);
                    }
                }
                render_schedule(view, day_of_week_names, cast_scheduleData_to_renderData(generate_schedule(LocalDate.parse(((TextView) view.findViewById(R.id.date)).getText()))), opened.toArray(new Integer[0]));
            }
        });

        scheduleListView.setAdapter(mAdapter);

        for (int group : opened) {
            scheduleListView.expandGroup(group);
        }

        setExpandableListViewHeightBasedOnChildren(scheduleListView);

        scheduleListView.setOnGroupExpandListener((groupPosition) -> {
            setExpandableListViewHeightBasedOnChildren(scheduleListView);
        });

        scheduleListView.setOnGroupCollapseListener((groupPosition) -> {
            setExpandableListViewHeightBasedOnChildren(scheduleListView);
        });

    }

    public void render_events(View view, String[][] childItems, Integer[] opened) {
        String NAME = "NAME";
        String TIME = "TIME";
        String DATE = "DATE";
        String DURATION = "DURATION";
        String PRIORITY = "PRIORITY";
        String FOR = "FOR";
        eventListView = (ExpandableListView) view.findViewById(R.id.eventExpandableListView );
        // create lists for group and child items
        List<Map<String, String>> groupData = new ArrayList<>();
        List<List<Map<String, String>>> childData = new ArrayList<>();
        // add data in group and child list
        for (String[] childItem : childItems) {
            Map<String, String> curGroupMap = new HashMap<>();
            groupData.add(curGroupMap);
            curGroupMap.put(NAME, childItem[0] + " " + childItem[3]);

            List<Map<String, String>> children = new ArrayList<>();
            Map<String, String> curChildMap = new HashMap<>();
            children.add(curChildMap);
            curChildMap.put(DATE, childItem[0]);
            curChildMap.put(TIME, get_time_as_string(Integer.parseInt(childItem[1])));
            curChildMap.put(DURATION, (Integer.parseInt(childItem[2]) / 60 == 0 ? "" : Integer.parseInt(childItem[2]) / 60 + " часов ") + Integer.parseInt(childItem[2]) % 60 + " минут");
            curChildMap.put(NAME, childItem[3]);
            curChildMap.put(PRIORITY, childItem[4]);
            ArrayList<String> event_for = new ArrayList<>();
            if (childItem[5].equals("_")) event_for.add("всем");
            else for (String i : childItem[5].split("-")) event_for.add(students[Integer.parseInt(i)].split(" ")[0]);
            curChildMap.put(FOR, String.join(", ", event_for));
            childData.add(children);
        }
        if (childItems.length == 0) {
            view.findViewById(R.id.nothingEvents).setVisibility(View.VISIBLE);
        }
        // define arrays for displaying data in Expandable list view
        String[] groupFrom = {NAME};
        int[] groupTo = {R.id.heading};
        String[] childFrom = {NAME, DATE, TIME, DURATION, PRIORITY, FOR};
        int[] childTo = {R.id.event_name, R.id.event_start_date, R.id.event_start_time, R.id.event_duration, R.id.event_priority, R.id.event_for};


        // Set up the adapter
        mAdapter = new SimpleExpandableListAdapter(context, groupData,
                R.layout.event_group,
                groupFrom, groupTo,
                childData, R.layout.event_item,
                childFrom, childTo);

        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                ArrayList<Integer> opened = new ArrayList<>();
                for (int group = 0; group < mAdapter.getGroupCount(); group++) {
                    if (eventListView.isGroupExpanded(group)) {
                        opened.add(group);
                    }
                }
                render_events(view, getFullEventsArray(), opened.toArray(new Integer[0]));
            }
        });

        eventListView.setAdapter(mAdapter);

        for (int group : opened) {
            eventListView.expandGroup(group);
        }

        setExpandableListViewHeightBasedOnChildren(eventListView);

        eventListView.setOnGroupExpandListener((groupPosition) -> {
            setExpandableListViewHeightBasedOnChildren(eventListView);
        });

        eventListView.setOnGroupCollapseListener((groupPosition) -> {
            setExpandableListViewHeightBasedOnChildren(eventListView);
        });

    }

    public static void setExpandableListViewHeightBasedOnChildren(ExpandableListView expandableListView){
        ExpandableListAdapter adapter = expandableListView.getExpandableListAdapter();
        if (adapter == null){
            return;
        }
        int totalHeight = expandableListView.getPaddingTop() + expandableListView.getPaddingBottom();
        int width = (int) (context.getResources().getDisplayMetrics().widthPixels - 30 * context.getResources().getDisplayMetrics().density);
        for (int i = 0 ; i < adapter.getGroupCount() ; i++){
            View groupItem = adapter.getGroupView(i, false, null, expandableListView);
            groupItem.measure(View.MeasureSpec.makeMeasureSpec(width,
                    View.MeasureSpec.EXACTLY), View.MeasureSpec
                    .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalHeight += groupItem.getMeasuredHeight();

            if (expandableListView.isGroupExpanded(i) ){
                for( int j = 0 ; j < adapter.getChildrenCount(i) ; j++) {

                    View listItem = adapter.getChildView(i, j, false, null, expandableListView);
                    listItem.setLayoutParams(new ViewGroup.LayoutParams(width, View.MeasureSpec.UNSPECIFIED));
                    listItem.measure(View.MeasureSpec.makeMeasureSpec(width,
                            View.MeasureSpec.EXACTLY), View.MeasureSpec
                            .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    totalHeight += listItem.getMeasuredHeight();

                }
                totalHeight += (adapter.getChildrenCount(i)) * expandableListView.getDividerHeight();
            }
        }

        ViewGroup.LayoutParams params = expandableListView.getLayoutParams();
        int height = totalHeight + expandableListView.getDividerHeight() * (adapter.getGroupCount() - 1);

        if (height < 10)
            height = 100;
        params.height = height;
        expandableListView.setLayoutParams(params);
        expandableListView.requestLayout();
    }

    public String get_time_as_string(int min) {return (Integer) min / 60 + ":" + (String.valueOf((Integer) min % 60).length() == 1 ? "0" : "") + (Integer) min % 60;}
}

