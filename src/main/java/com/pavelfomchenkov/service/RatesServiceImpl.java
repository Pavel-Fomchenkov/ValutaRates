package com.pavelfomchenkov.service;

import com.pavelfomchenkov.dto.ValutaMidDTO;
import com.pavelfomchenkov.dto.ValutaRateDTO;
import com.pavelfomchenkov.mapper.ValutaMapper;
import com.pavelfomchenkov.model.Storage;
import com.pavelfomchenkov.model.Valuta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RatesServiceImpl implements RatesService {
    private final Storage storage;
    private final ValutaMapper mapper;

    @Override
    public void loadData(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        LocalDate date = start;
        while (date.isBefore(end.plusDays(1))) {
            if (!storage.getStorage().containsKey(date)) {
                byte[] content = loadDay(date);
                if (content != null) {
                    storage.getStorage().put(date, parseDay(content));
                }
            }
            date = date.plusDays(1);
        }
    }

    @Override
    public Collection<ValutaRateDTO> getMinRate(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        List<ValutaRateDTO> result = new ArrayList<>();
        try {
            while (start.isBefore(end.plusDays(1))) {
                LocalDate date = start;
                storage.getStorage().get(start).stream() // получили значение в виде коллекции валют
                        .map(valuta -> mapper.mapToValutaRateDTO(date, valuta))
                        .forEach(dto -> {
                            if (!result.contains(dto)) {
                                result.add(dto);
                            } else if (
                                    dto.getVUnitRate() < result.stream().filter(el -> el.getName().equals(dto.getName())).findAny().get().getVUnitRate()) {
                                ValutaRateDTO v = result.stream().filter(el -> el.getName().equals(dto.getName())).findAny().get();
                                v.setVUnitRate(dto.getVUnitRate());
                                v.setDate(date);
                            }
                        });
                start = start.plusDays(1);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public Collection<ValutaRateDTO> getMaxRate(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        List<ValutaRateDTO> result = new ArrayList<>();
        try {
            while (start.isBefore(end.plusDays(1))) {
                LocalDate date = start;
                storage.getStorage().get(start).stream() // получили значение в виде коллекции валют
                        .map(valuta -> mapper.mapToValutaRateDTO(date, valuta))
                        .forEach(dto -> {
                            if (!result.contains(dto)) {
                                result.add(dto);
                            } else if (
                                    dto.getVUnitRate() > result.stream().filter(el -> el.getName().equals(dto.getName())).findAny().get().getVUnitRate()) {
                                ValutaRateDTO v = result.stream().filter(el -> el.getName().equals(dto.getName())).findAny().get();
                                v.setVUnitRate(dto.getVUnitRate());
                                v.setDate(date);
                            }
                        });
                start = start.plusDays(1);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public Collection<ValutaMidDTO> getMidRate(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        TreeMap<String, List<Double>> rates = new TreeMap<>();
        while (start.isBefore(end.plusDays(1))) {
            for (Valuta v : storage.getStorage().get(start)) {
                if (rates.containsKey(v.getName())) {
                    rates.get(v.getName())
                            .add(v.getVUnitRate());
                } else {
                    rates.put(v.getName(), new ArrayList<>(List.of(v.getVUnitRate())));
                }
            }
            start = start.plusDays(1);
        }
        List<ValutaMidDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : rates.entrySet()) {
            Double midValue = entry.getValue().stream().reduce(Double::sum).get() / entry.getValue().size();
            result.add(new ValutaMidDTO(midValue, entry.getKey()));
        }
        return result;
    }

    private byte[] loadDay(LocalDate date) {
        String dateString = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String urlString = "http://www.cbr.ru/scripts/XML_daily_eng.asp?date_req=" + dateString;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                int totalBytesRead = 0;
                final int maxSize = 1_000_000; // 1 МБ

                while ((bytesRead = is.read(buffer)) != -1) {
                    if (totalBytesRead + bytesRead > maxSize) {
                        throw new IOException("Размер загружаемых данных превышает лимит.");
                    }
                    baos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                is.close(); // закрытие буфера
                System.out.printf("XML от %s добавлен в хранилище данных.\n", dateString);
                return baos.toByteArray();
            } else {
                System.out.println("Ошибка HTTP: " + responseCode);
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Valuta> parseDay(byte[] dayData) {
        List<Valuta> valutas = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document =
                    builder.parse(new ByteArrayInputStream(dayData));
            // Извлечение данных из элемента
            NodeList nodeList = document.getElementsByTagName("Valute");

            for (int i = 0; i < nodeList.getLength(); i++) {
                // Извлечение данных по каждой валюте из элемента
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Valuta valuta = new Valuta();
                    Element element = (Element) node;
                    valuta.setValuteID(element.getAttribute("ID")); // присваиваем valuteID

                    NodeList nodeChildren = node.getChildNodes();
                    for (int j = 0; j < nodeChildren.getLength(); j++) {
                        Node subnode = nodeChildren.item(j);
                        if (subnode.getNodeType() == Node.ELEMENT_NODE) {
                            Element subelement = (Element) subnode;
                            String parameterName = subelement.getNodeName();
                            String parameterContent = subelement.getFirstChild().getTextContent();

                            switch (parameterName) {
                                case "NumCode":
                                    valuta.setNumCode(Integer.parseInt(parameterContent));
                                    break;
                                case "CharCode":
                                    valuta.setCharCode(parameterContent);
                                    break;
                                case "Nominal":
                                    valuta.setNominal(Integer.parseInt(parameterContent));
                                    break;
                                case "Name":
                                    valuta.setName(parameterContent);
                                    break;
                                case "Value":
                                    valuta.setValue(Double.parseDouble(parameterContent.replace(",", ".")));
                                    break;
                                case "VunitRate":
                                    valuta.setVUnitRate(Double.parseDouble(parameterContent.replace(",", ".")));
                                    break;
                                default:
                                    throw new RuntimeException();
                            }
                        }
                    }
                    valutas.add(valuta);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Не могу собрать документ из переданных данных");
        }
        return valutas;
    }
}
