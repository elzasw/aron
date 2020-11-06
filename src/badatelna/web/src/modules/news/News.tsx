import React from "react";

export const News: React.FC = () => {
  return (
    <div className="news padding-big">
      {[
        {
          date: "1.2.2020",
          label: "Nadpis aktuality 3",
          text:
            "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Fusce suscipit libero eget elit. Fusce tellus odio, dapibus id fermentum quis, suscipit id erat. Vivamus porttitor turpis ac leo. Maecenas libero. Duis pulvinar. Suspendisse sagittis ultrices augue. Duis pulvinar. Nullam justo enim, consectetuer nec, ullamcorper ac, vestibulum in, elit. Etiam dictum tincidunt diam. Nulla quis diam. Duis pulvinar. Proin mattis lacinia justo. Fusce tellus. Aliquam ornare wisi eu metus.",
          link: true,
        },
        {
          date: "5.1.2020",
          label: "Nadpis aktuality 2",
          text:
            "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Fusce suscipit libero eget elit. Fusce tellus odio, dapibus id fermentum quis, suscipit id erat. Vivamus porttitor turpis ac leo. Maecenas libero. Duis pulvinar. Suspendisse sagittis ultrices augue. Duis pulvinar. Nullam justo enim, consectetuer nec, ullamcorper ac, vestibulum in, elit. Etiam dictum tincidunt diam. Nulla quis diam. Duis pulvinar. Proin mattis lacinia justo. Fusce tellus. Aliquam ornare wisi eu metus.",
        },
        {
          date: "3.12.2020",
          label: "Nadpis aktuality 1",
          text:
            "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Fusce suscipit libero eget elit. Fusce tellus odio, dapibus id fermentum quis, suscipit id erat. Vivamus porttitor turpis ac leo. Maecenas libero. Duis pulvinar. Suspendisse sagittis ultrices augue. Duis pulvinar. Nullam justo enim, consectetuer nec, ullamcorper ac, vestibulum in, elit. Etiam dictum tincidunt diam. Nulla quis diam. Duis pulvinar. Proin mattis lacinia justo. Fusce tellus. Aliquam ornare wisi eu metus.",
        },
      ].map(({ date, label, text, link }) => (
        <div key={date} className="margin-bottom-big">
          <div className="margin-bottom-tiny">
            {date}&nbsp;<span style={{ fontWeight: 600 }}>{label}</span>
          </div>
          <div className="margin-bottom-tiny">{text}</div>
          {link ? (
            <>
              <div className="margin-bottom-tiny">
                <a href="#odkaz1">Odkaz na soubor 1</a>
              </div>
              <div>
                <a href="#odkaz2">Odkaz na soubor 2</a>
              </div>
            </>
          ) : (
            <></>
          )}
        </div>
      ))}
    </div>
  );
};
