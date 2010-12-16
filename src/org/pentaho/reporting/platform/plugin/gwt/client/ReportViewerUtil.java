package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.pentaho.gwt.widgets.client.utils.i18n.ResourceBundle;
import org.pentaho.gwt.widgets.client.utils.string.StringTokenizer;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;

/**
 * Todo: Document me!
 * <p/>
 * Date: 22.07.2010
 * Time: 11:11:15
 *
 * @author Thomas Morgner.
 */
public class ReportViewerUtil
{
  private ReportViewerUtil()
  {
  }

  /**
   * @noinspection HardCodedStringLiteral
   */
  public static String normalizeParameterValue(final Parameter parameter, String type, final String selection)
  {
    if (selection == null || selection.length() == 0)
    {
      return null;
    }

    if (type == null)
    {
      return selection;
    }

    if (type.startsWith("[L") && type.endsWith(";")) // NON-NLS
    {
      type = type.substring(2, type.length() - 1);
    }

    if ("java.util.Date".equals(type) ||
        "java.sql.Date".equals(type) ||
        "java.sql.Time".equals(type) ||
        "java.sql.Timestamp".equals(type))
    {
      try
      {
        // date handling speciality here ...
        final String timezone = parameter.getAttribute("timezone");
        String timezoneHint = parameter.getTimezoneHint();
        if (timezone == null || "server".equals(timezone))
        {
          if (timezoneHint == null)
          {
            timezoneHint = extractTimezoneHintFromData(selection);
          }
          if (timezoneHint == null)
          {
//            Window.alert("No Timezone hint given for " + parameter.getName());
            return selection;
          }

          // update the parameter definition, so that the datepickerUI can work properly ...
          parameter.setTimezoneHint(timezoneHint);
//          Window.alert("No Timezone given for " + parameter.getName());
          return selection;
        }

        if ("client".equals(timezone))
        {
          return selection;
        }

        // for every other mode (fixed timezone modes), translate the time into the specified timezone
        if (timezoneHint != null && timezoneHint.length() > 0)
        {
          if (selection.endsWith(timezoneHint))
          {
            return selection;
          }
//          Window.alert("Selection is not in same TZ " + parameter.getName() + " " + timezoneHint + " " + selection);
        }
        else
        {
//          Window.alert("TZ Hint " + parameter.getName() + " " + timezoneHint + " " + selection);
        }

        // the resulting time will have the same universal time as the original one, but the string
        // will match the timeoffset specified in the timezone.
        return convertTimeStampToTimeZone(selection, TimeZoneOffsets.getInstance().getOffset(timezone));
      }
      catch (IllegalArgumentException iae)
      {
        // failed to parse the text ..
        return selection;
      }
    }

    return selection;
  }

  public static Date parseWithTimezone(final String dateString)
  {
    if (dateString.length() != 28)
    {
      throw new IllegalArgumentException("This is not a valid ISO-date with timezone: " + dateString);
    }
    return DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(dateString);
  }

  public static Date parseWithoutTimezone(String dateString)
  {
    if (dateString.length() == 28)
    {
      dateString = dateString.substring(0, 23);
    }
    if (dateString.length() != 23)
    {
      throw new IllegalArgumentException("This is not a valid ISO-date without timezone: " + dateString);
    }
    return DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(dateString);
  }

  /**
   * Converts a time from a arbitary timezone into the local timezone. The timestamp value remains unchanged,
   * but the string representation changes to reflect the give timezone.
   *
   * @param originalTimestamp the timestamp as string from the server.
   * @param targetTimeZoneOffsetInMinutes the target timezone offset in minutes from GMT
   * @return the converted timestamp string.
   */
  public static String convertTimeStampToTimeZone(final String originalTimestamp,
                                                  final int targetTimeZoneOffsetInMinutes)
  {
    final DateTimeFormat localDate = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    final Date dateLocal = parseWithoutTimezone(originalTimestamp);
    final Date dateUtc = parseWithTimezone(originalTimestamp);
    final String offsetText = TimeZoneOffsets.formatOffset(targetTimeZoneOffsetInMinutes);
    final long date = dateLocal.getTime() + (targetTimeZoneOffsetInMinutes * 60000) +
        (dateUtc.getTime() - dateLocal.getTime()) - (getNativeTimezoneOffset() * 60000);

//    Window.alert("Converting: LocalDate:" + dateLocal + " vs UTC:" + dateUtc +
//        " \n Offset:" + offsetText + " Min:" + targetTimeZoneOffsetInMinutes +
//        " \n Native: " + getNativeTimezoneOffset());
    final Date localWithShift = new Date(date);
    final String dateAsText = localDate.format(localWithShift) + offsetText;
    return dateAsText;
  }

  /**
   * Returns the current native time-zone offset from UTC to local time.
   *
   * @return the offset in minutes.
   */
  public static native int getNativeTimezoneOffset()
    /*-{
      return -(new Date().getTimezoneOffset());
    }-*/;

  public static String extractTimezoneHintFromData(final String dateString)
  {
    if (dateString.length() == 28)
    {
      return dateString.substring(23, 28);
    }
    else
    {
      return null;
    }
  }


  /**
   * Parses the history tokens and returns a map keyed by the parameter names. The parameter values are
   * stored as list of strings.
   *
   * @return the token map.
   */
  public static ParameterValues getHistoryTokenParameters()
  {

    final ParameterValues map = new ParameterValues();
    final String historyToken = History.getToken();
    if (StringUtils.isEmpty(historyToken))
    {
      return map;
    }

    final StringTokenizer st = new StringTokenizer(historyToken, "&"); //$NON-NLS-1$
    final int paramTokens = st.countTokens();
    for (int i = 0; i < paramTokens; i++)
    {
      final String fullParam = st.tokenAt(i);

      final StringTokenizer st2 = new StringTokenizer(fullParam, "="); //$NON-NLS-1$
      if (st2.countTokens() != 2)
      {
        continue;
      }

      final String name = URL.decodeComponent(st2.tokenAt(0));
      final String value = URL.decodeComponent(st2.tokenAt(1));
      map.addSelectedValue(name, value);
    }
    // tokenize this guy & and =
    return map;
  }

  /**
   * Builds the URL that is needed to communicate with the backend.
   *
   * @param renderType         the render type, never null.
   * @param reportParameterMap the parameter map, never null.
   * @return the generated URL
   */
  public static String buildReportUrl(final ReportViewer.RENDER_TYPE renderType,
                                      final ParameterValues reportParameterMap)
  {

    if (reportParameterMap == null)
    {
      throw new NullPointerException();
    }
    String reportPath = Window.Location.getPath();
    if (reportPath.indexOf("reportviewer") != -1) //$NON-NLS-1$
    {
      reportPath = reportPath.substring(0, reportPath.indexOf("reportviewer") - 1); //$NON-NLS-1$
      // add query part of url
      reportPath += "?"; //$NON-NLS-1$
    }
    reportPath += "renderMode=" + renderType; // NON-NLS

    final ParameterValues parameters = new ParameterValues();

    // User submitted values always make it into the final URL ..
    for (final String key : reportParameterMap.getParameterNames())
    {
      if ("renderMode".equals(key))
      {
        continue;
      }

      final String[] valueList = reportParameterMap.getParameterValues(key);
      final String[] encodedList = new String[valueList.length];
      for (int i = 0; i < valueList.length; i++)
      {
        final String value = valueList[i];
        if (value == null)
        {
          encodedList[i] = (""); //$NON-NLS-1$
        }
        else
        {
          encodedList[i] = (value);
        }
      }
      // Window.alert("Paramter-Value: " + key);
      parameters.setSelectedValues(key, encodedList);
    }


    // history token parameters will override default parameters (already on URL)
    // but they will not override user submitted parameters
    final ParameterValues historyParams = getHistoryTokenParameters();
    if (historyParams != null)
    {
      for (final String key : historyParams.getParameterNames())
      {
        if (parameters.containsParameter(key))
        {
          continue;
        }
        if ("renderMode".equals(key))
        {
          continue;
        }

        final String[] valueList = historyParams.getParameterValues(key);
        final String[] encodedList = new String[valueList.length];
        for (int i = 0; i < valueList.length; i++)
        {
          final String value = valueList[i];
          if (value == null)
          {
            encodedList[i] = (""); //$NON-NLS-1$
          }
          else
          {
            encodedList[i] = (value);
          }
        }
        // Window.alert("History-Value: " + key);
        parameters.setSelectedValues(key, encodedList);
      }
    }

    // Last but not least - add the paramters that were given in the URL ...
    // The value is decoded, the parameter name is not decoded (according to the source code for GWT-2.0).
    final Map<String, List<String>> requestParams = Window.Location.getParameterMap();
    if (requestParams != null)
    {
      for (final String rawkey : requestParams.keySet())
      {
        final String key = URL.decodeComponent(rawkey);
        if ("renderMode".equals(key))
        {
          continue;
        }

        if (parameters.containsParameter(key))
        {
          continue;
        }

        final List<String> valueList = requestParams.get(rawkey);
        final String[] encodedList = new String[valueList.size()];
        for (int i = 0; i < valueList.size(); i++)
        {
          final String value = valueList.get(i);
          if (value == null)
          {
            encodedList[i] = (""); //$NON-NLS-1$
          }
          else
          {
            encodedList[i] = (value);
          }
        }
        //  Window.alert("Location-Value: " + key);
        parameters.setSelectedValues(key, encodedList);
      }
    }

    final String parametersAsString = parameters.toURL();
    if (History.getToken().equals(parametersAsString) == false)
    {
      // don't add duplicates, only new ones
      // assuming that History.getToken() returns the last newItem string unchanged,
      // then we must not URL-encode the paramter string.
      History.newItem(parametersAsString, false);
    }

    reportPath += "&" + parametersAsString;
    if (GWT.isScript() == false)
    {
      System.out.println("Computed path was: " + reportPath);
      reportPath = reportPath.substring(1);
      reportPath = "?solution=steel-wheels&path=reports&name=Inventory.prpt&" + reportPath; //$NON-NLS-1$
      final String url = "http://localhost:8080/pentaho/content/reporting" + reportPath + "&userid=joe&password=password"; //$NON-NLS-1$ //$NON-NLS-2$
      System.out.println("Using development url: " + url);
      return url;
    }
/*    else
    {
      Window.alert("Computed-URL: " + reportPath);
    }
    */
    return reportPath;
  }


  public static native boolean isInPUC()
    /*-{
      return (top.mantle_initialized == true);
    }-*/;

  public static native void showPUCMessageDialog(String title, String message)
    /*-{
      top.mantle_showMessage(title, message);
    }-*/;

  public static void showErrorDialog(final ResourceBundle messages, final String error)
  {
    final String title = messages.getString("error", "Error");//$NON-NLS-1$
    showMessageDialog(messages, title, error);
  }

  public static int parseInt(final String text, final int defaultValue)
  {
    if (ReportViewerUtil.isEmpty(text))
    {
      return defaultValue;
    }
    try
    {
      return Integer.parseInt(text);
    }
    catch (NumberFormatException nfe)
    {
      return defaultValue;
    }
  }

  public static void showMessageDialog(final ResourceBundle messages, final String title, final String message)
  {
    if (ReportViewerUtil.isInPUC())
    {
      ReportViewerUtil.showPUCMessageDialog(title, message);
      return;
    }

    final DialogBox dialogBox = new DialogBox(false, true);
    dialogBox.setText(title);
    final VerticalPanel dialogContent = new VerticalPanel();
    DOM.setStyleAttribute(dialogContent.getElement(), "padding", "0px 5px 0px 5px"); //$NON-NLS-1$ //$NON-NLS-2$
    dialogContent.add(new HTML(message, true));
    final HorizontalPanel buttonPanel = new HorizontalPanel();
    DOM.setStyleAttribute(buttonPanel.getElement(), "padding", "0px 5px 5px 5px"); //$NON-NLS-1$ //$NON-NLS-2$
    buttonPanel.setWidth("100%"); //$NON-NLS-1$
    buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    final Button okButton = new Button(messages.getString("ok", "OK")); //$NON-NLS-1$ //$NON-NLS-2$
    okButton.addClickHandler(new ClickHandler()
    {
      public void onClick(final ClickEvent event)
      {
        dialogBox.hide();
      }
    });
    buttonPanel.add(okButton);
    dialogContent.add(buttonPanel);
    dialogBox.setWidget(dialogContent);
    dialogBox.center();
    // prompt
  }

  public static boolean isEmpty(final String text)
  {
    return text == null || "".equals(text);
  }

}